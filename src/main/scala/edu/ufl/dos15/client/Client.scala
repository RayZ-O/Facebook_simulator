package edu.ufl.dos15.client

import java.util.Base64
import java.security.PublicKey
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import akka.actor.{Actor, ActorLogging, Cancellable}
import spray.http.{StatusCodes, HttpRequest, HttpResponse}
import spray.client.pipelining._
import scala.util.{Success, Failure, Random}
import scala.concurrent.Future
import scala.concurrent.duration._
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization
import com.roundeights.hasher.Implicits._
import edu.ufl.dos15.fbapi.Json4sProtocol
import edu.ufl.dos15.crypto.Crypto._

class Client(id: String, host: String, port: Int, page: Boolean) extends Actor
    with ActorLogging with Json4sProtocol {
  import context.dispatcher

  import edu.ufl.dos15.fbapi.UserService._
  import edu.ufl.dos15.fbapi.PageService._
  import edu.ufl.dos15.fbapi.FeedService._
  import edu.ufl.dos15.fbapi.FeedService.PrivacyType._
  import edu.ufl.dos15.fbapi.FeedService.FeedType._
  import edu.ufl.dos15.fbapi.FriendListService._
  import edu.ufl.dos15.fbapi.FriendListService.FriendListType._
  import edu.ufl.dos15.fbapi.FBMessage.HttpIdReply
  import edu.ufl.dos15.fbapi.FBMessage.HttpSuccessReply
  import edu.ufl.dos15.fbapi.FBMessage.HttpListReply
  import edu.ufl.dos15.fbapi.FBMessage.HttpDataReply
  import edu.ufl.dos15.fbapi.FBMessage.EncryptedData
  import edu.ufl.dos15.fbapi.FBMessage.Tick

  val userUri = s"http://$host:$port/user"
  val pageUri = s"http://$host:$port/page"
  val feedUri = s"http://$host:$port/feed"
  val friendUri = s"http://$host:$port/friends"
  val edgeBaseUri = if (page == true) s"http://$host:$port/page/$id"
                else s"http://$host:$port/user/$id"

  private val kp = RSA.generateKeyPair()
  val pubKey = kp.getPublic()
  private val priKey = kp.getPrivate()
  import scala.collection.mutable.HashMap
  var friendsPubKeys = new HashMap[String, PublicKey]

  import scala.collection.mutable.ArrayBuffer
  var myPost = new ArrayBuffer[String]
  var myFriends = new ArrayBuffer[String]
  var myFriendListId = ""
  var tick: Cancellable = _

  val pipeline = sendReceive

  def receive: Receive = {
    case Run(ids) =>
      val f = createFriends()
      ids foreach {friendId => if (friendId != id) myFriends += friendId }
      f onComplete {
      case Success(rep) =>
        myFriendListId = rep.id
        updateFriends()
      case Failure(error) =>
        log.error(error, "Couldn't post friend")
    }
      tick = context.system.scheduler.schedule(1.second, 1.second, self, Tick)

    case Tick => takeAction()

    case _ =>
  }

  def takeAction() = {
    val n = Random.nextInt(100)
    if (n < 30 ) {
      readNewFeeds()
    } else if (n < 60) {
      createFeed()
    } else if (n < 80) {
      readFriendProfile()
    } else if (n < 90) {
      updateFeed()
    } else if (n < 95) {
      updateUser()
    } else {
      deleteFeed()
    }
  }

  def encyptData(data: String, symKey: SecretKey, iv: IvParameterSpec) = {
    val digest = data.sha256
    val signature = RSA.sign(digest.bytes, priKey)
    val dataWithSign = data + "|" +  new String(Base64.getEncoder().encodeToString(signature))
    val encyptData = AES.encrypt(dataWithSign, symKey, iv)
    val keyBytes = symKey.getEncoded()
    val keys = friendsPubKeys.map{ p => (p._1 -> RSA.encrypt(keyBytes, p._2)) }
    keys += id -> RSA.encrypt(keyBytes, pubKey)
    EncryptedData(encyptData, iv.getIV(), keys)
  }

  def decyptData(encryptedData: Array[Byte], encryptedKey: Array[Byte], iv: Array[Byte]) = {
    val secKey = AES.decodeKey(RSA.decrypt(encryptedKey, priKey))
    val dataWithSign = new String(AES.decrypt(encryptedData, secKey, new IvParameterSpec(iv)))
    dataWithSign.split("\\|") match {
      case Array(data, sign) =>
        val signature = Base64.getDecoder().decode(sign)
        RSA.verify(data.sha256.bytes, signature, pubKey) match {
          case true => (true, data)
          case false => (false, "Unmatching digital signature")
        }
      case _ => (false, "Unrecognized encrypted data")
    }
  }

  def filterFields(nodeId: String, params: Option[String], content: String): JsonAST.JValue = {
    import org.json4s.JsonDSL._
    val json = (JObject() ~ ("id" -> nodeId)) merge parse(content)
    params match {
      case Some(fields) =>
        val query = fields.split(",")
        query.foldLeft(JObject()) { (res, name) =>
          (res ~ (name -> json \ name)) }
      case None => json
    }
  }

  def getData(req: HttpRequest, errMsg: String) = {
    val pipeline = sendReceive ~> unmarshal[HttpDataReply]
    val responseFuture = pipeline(req)
    val nodeId = req.uri.query.get("id")
    val params = req.uri.query.get("fields")
    responseFuture onComplete {
      case Success(ed) =>
        try {
          val res = decyptData(ed.data, ed.iv.get, ed.key.get)
          res._1 match {
            case true =>
              val json = filterFields(nodeId.get, params, res._2)
              val jsonStr = compact(render(json))
              println(jsonStr)
            case false => log.error(res._2)
          }
        } catch {
          case error: Throwable => log.error(error, "Decrypt failed")
        }
      case Failure(error) =>
        log.error(error, errMsg)
    }
  }

  def readFriendProfile() = {
    val f = getFriends(myFriendListId)
    f onComplete {
      case Success(friends) =>
        friends.data.getOrElse(List()) foreach { friendId => getUser(friendId) }
      case Failure(error) =>
        log.error(error, "Couldn't read friend profile")
    }
  }

  def readNewFeeds() = {
    val f = getNewFeeds()
    f onComplete {
      case Success(feeds) =>
        feeds.list foreach { feedId => getFeed(feedId) }
      case Failure(error) =>
        log.error(error, "Couldn't read new feeds")
    }
  }

  def getUser(userId: String) = {
    val responseFuture = pipeline { Get(userUri + "/" + userId) }
    responseHandler(responseFuture,
                    s"Get user $userId failed",
                    s"Couldn't get user $userId")
  }

  def updateUser() = {
    val user = generateUpdate("user")
    val responseFuture = pipeline { Put(userUri + "/" + id, user) }
    responseHandler(responseFuture,
                    s"Update user $id failed",
                    s"Couldn't put user $id ")
  }

  def deleteUser() = {
    val responseFuture = pipeline { Delete(userUri + "/" + id) }
    stopHandler(responseFuture,
                s"Delete user $id failed",
                s"Couldn't delete user $id")
  }

  def getPage(pageId: String) = {
    val responseFuture = pipeline { Get(pageUri + "/" + pageId) }
    responseHandler(responseFuture,
                    s"Get page $pageId failed",
                    s"Couldn't get page $pageId")
  }

  def createPage() = {
    val page = generateCreate("page")
    val pipeline = sendReceive ~> unmarshal[HttpIdReply]
    val responseFuture = pipeline { Post(pageUri, page)  }
    responseFuture onComplete {
      case Success(rep) =>
        context.actorSelection("/user/simulator") ! Register(rep.id)
      case Failure(error) =>
        log.error(error, "Couldn't post page")
    }
  }

  def updatePage() = {
    val page = generateUpdate("page")
    val responseFuture = pipeline { Put(pageUri + "/" + id, page) }
    responseHandler(responseFuture,
                    s"Update page $id failed",
                    s"Couldn't put page $id")
  }

  def deletePage() = {
    val responseFuture = pipeline { Delete(pageUri + "/" + id) }
    stopHandler(responseFuture,
                s"Delete page $id failed",
                s"Couldn't delete page $id")
  }

  def getNewFeeds() = {
    val pipeline = sendReceive ~> unmarshal[HttpListReply]
    pipeline { Get(feedUri + "/" + "new") }
  }

  def getFeed(feedId: String) = {
    val responseFuture = pipeline { Get(feedUri + "/" + feedId) }
    responseHandler(responseFuture,
                s"Get feed failed",
                s"Couldn't get feed")
  }

  def createFeed() = {
    val pipeline = sendReceive ~> unmarshal[HttpIdReply]
    val feed = generateCreate("feed")
    val responseFuture = pipeline { Post(edgeBaseUri + "/" + "feed", feed) }
    responseFuture onComplete {
      case Success(rep) =>
        myPost += rep.id
      case Failure(error) =>
        log.error(error, "Couldn't get new feed")
    }
  }

  def updateFeed() = {
    if (myPost.length > 0) {
      val feedId = myPost(Random.nextInt(myPost.length))
      val feed = generateUpdate("feed")
      val responseFuture = pipeline { Put(feedUri + "/" + feedId, feed) }
      responseHandler(responseFuture,
                  s"Update feed failed",
                  s"Couldn't put feed")
     }
  }

  def deleteFeed() = {
    if (myPost.length > 0) {
      val feedId = myPost(Random.nextInt(myPost.length))
      val pipeline = sendReceive ~> unmarshal[HttpSuccessReply]
      val responseFuture = pipeline { Delete(feedUri + "/" + feedId) }
      responseFuture onComplete {
        case Success(res) =>
          myPost -= feedId
        case Failure(error) =>
          log.error(error, "Couldn't delete feed")
      }
    }
  }

  def getFriends(friendListId: String) = {
    val pipeline = sendReceive ~> unmarshal[FriendList]
    pipeline { Get(friendUri + "/" + friendListId) }
  }

  def createFriends() = {
    val friends = generateCreate("friends")
    val pipeline = sendReceive ~> unmarshal[HttpIdReply]
    pipeline { Post(edgeBaseUri + "/" + "friends", friends) }
  }

  def updateFriends() = {
    if (!myFriends.isEmpty) {
      val ids = myFriends.reduceLeft(_+","+_)
      val responseFuture = pipeline { Put(friendUri + "/" + myFriendListId + "?ids=" + ids) }
      responseHandler(responseFuture,
                      s"Put friends to friend list $myFriendListId failed",
                      s"Couldn't put friends to friend list $myFriendListId")
    }
  }

  def deleteFriends() = {
    val responseFuture = pipeline { Delete( friendUri + "/" + myFriendListId) }
    responseHandler(responseFuture,
                    s"Delete friend list $myFriendListId failed",
                    s"Couldn't delete friend list $myFriendListId")
  }

  def responseHandler(f: Future[HttpResponse], errMsg1: String, errMsg2: String) = {
    f onComplete {
      case Success(res) =>
        if (res.status != StatusCodes.OK) {
          log.error(errMsg1)
        } else {
          println(res)
        }
      case Failure(error) =>
        log.error(error, errMsg2)
    }
  }

  def stopHandler(f: Future[HttpResponse], errMsg1: String, errMsg2: String) = {
    f onComplete {
      case Success(res) =>
        if (res.status == StatusCodes.OK) {
          context.stop(self)
        } else {
          log.error(errMsg1)
        }
      case Failure(error) =>
        log.error(error, errMsg2)
    }
  }

  def generateCreate(t: String) = {
     t match {
      case "page" =>
        Page(name=Some(randomString(8)),
             description=Some(randomString(20)),
             link=Some("http://" + randomString(6) + ".com"),
             location=Some(Location(city=Some(randomString(5)),
                                    country=Some(randomString(5)))))

      case "feed" =>
        Random.nextInt(3) match {
          case 0 => Feed(message=Some(randomString(20)),
                         privacy=Some(EVERYONE),
                         feed_type=Some(STATUS))

          case 1 => Feed(name=Some(randomString(8)),
                         link=Some("http://www." + randomString(6) + ".com"),
                         privacy=Some(EVERYONE),
                         feed_type=Some(LINK))

          case 2 => Feed(source=Some("http://www." + randomString(6) + ".com"),
                         privacy=Some(EVERYONE),
                         feed_type=Some(VIDEO))
        }

      case "friends" =>
        FriendList(name=Some(randomString(8)),
                   list_type=Some(CLOSE_FRIENDS))
    }
  }

  def generateUpdate(t: String) = {
    t match {
      case "user" =>
        User(email=Some(randomString(8)))

      case "page" =>
        Page(name=Some(randomString(8)),
             description=Some(randomString(20)))

      case "feed" =>
        val message = Some(randomString(50))
        Feed(message=message)

      case "friends" =>
        val name = Some(randomString(8))
        FriendList(name=name)
    }
  }

  def randomString(length: Int) = {
    Random.alphanumeric.take(length).mkString
  }
}


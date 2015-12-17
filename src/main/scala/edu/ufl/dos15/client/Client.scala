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
import scala.concurrent.Await

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
  import edu.ufl.dos15.fbapi.FBMessage.RegisterCred
  import edu.ufl.dos15.fbapi.FBMessage.CheckNonce
  import edu.ufl.dos15.fbapi.FBMessage.UpdatedData
  import edu.ufl.dos15.fbapi.FBMessage.Tick


  val baseUri = s"http://$host:$port"
  val userUri = baseUri + "/user"
  val pageUri = baseUri + "/page"
  val feedUri = baseUri + "/feed"
  val friendUri = baseUri + "/friends"
  
  private val kp = RSA.generateKeyPair()
  val pubKey = kp.getPublic()
  private val priKey = kp.getPrivate()
  val serverPubKey = RSA.generateKeyPair().getPublic()//TODO
  import scala.collection.mutable.HashMap
  var friendsPubKeys = new HashMap[String, PublicKey]

  import scala.collection.mutable.ArrayBuffer
  var token = ""
  var myPost: List[String] = _
  var myFriends: List[String] = _
  var myFriendListIds: List[String] = _
  var tick: Cancellable = _

  def receive: Receive = {
    case Run(ids) =>
//      val f = createFriends()
//      ids foreach {friendId => if (friendId != id) myFriends += friendId }
//      f onComplete {
//      case Success(rep) =>
//        myFriendListId = rep.id
//        updateFriends()
//      case Failure(error) =>
//        log.error(error, "Couldn't post friend")
//    }
      tick = context.system.scheduler.schedule(1.second, 1.second, self, Tick)

    case Tick => takeAction()

    case _ =>
  }

  def takeAction() = {
    val n = Random.nextInt(100)
    if (n < 30 ) {
      getNewFeeds()
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

  def register(): Future[Option[String]] = {
    val name = randomString(20)
    val passwd = randomString(12).bcrypt.hex
    val encrypted = RSA.encrypt(name + "|" + passwd, serverPubKey)
    val reg = RegisterCred(encrypted, pubKey.getEncoded())
    val pipeline = sendReceive ~> unmarshal[HttpIdReply]
    val responseFuture = pipeline( Post(baseUri + "/register", reg) )
    responseFuture.map {
      case HttpIdReply(id) => Some(id)
    }.recover {
      case _ => None
    }
  }

  def getNonce(): Future[Option[String]] = {
    val pipeline = sendReceive ~> unmarshal[HttpDataReply]
    val responseFuture = pipeline( Get(baseUri + "/login") )
    responseFuture.map {
      case HttpDataReply(data, _, _) => Some(new String(RSA.decrypt(data, priKey)))
    }.recover {
      case _ => None
    }
  }

  def login(): Option[String] = {
    Await.ready(getNonce(), Duration.Inf).value.get match {
      case Success(n) =>
        n match {
          case Some(nonce) => login(nonce)
          case None =>
            log.info("Get nonce failed")
            None
        }
      case Failure(e) =>
        log.info(e.toString() + "Get nonce failed")
        None
    }
  }

  def login(nonce: String): Option[String] = {
    val pipeline = sendReceive ~> unmarshal[HttpDataReply]
    val sign = RSA.sign(nonce, priKey)
    val cn = CheckNonce(nonce, sign)
    val responseFuture = pipeline( Post(baseUri + "/login/pubkey", cn))
    Await.ready(responseFuture, Duration.Inf).value.get match {
      case Success(dr) => Some(new String(RSA.decrypt(dr.data, priKey)))
      case Failure(e) =>
        log.info(e.toString() + "Login failed")
        None
    }
  }

  def encyptData(data: String) = {
    val digest = data.sha256
    val signature = RSA.sign(digest.bytes, priKey)
    val dataWithSign = data + "|" +  new String(Base64.getEncoder().encodeToString(signature))
    val iv = AES.generateIv()
    val symKey = AES.generateKey()
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

  def getData(nodeId: String, req: HttpRequest, errMsg: String) = {
    val pipeline = addHeader("ACCESS-TOKEN", token) ~>
                   sendReceive ~>
                   unmarshal[HttpDataReply]
    val responseFuture = pipeline(req)
    val params = req.uri.query.get("fields")
    responseFuture onComplete {
      case Success(ed) =>
        try {
          val res = decyptData(ed.data, ed.iv.get, ed.key.get)
          res._1 match {
            case true =>
              val json = filterFields(nodeId, params, res._2)
              val jsonStr = compact(render(json))
              println(jsonStr)
            case false => log.info(res._2)
          }
        } catch {
          case error: Throwable => log.info(error.toString() + "Decrypt failed")
        }
      case Failure(error) =>
        log.info(error.toString() + errMsg)
    }
  }

  def postData(req: HttpRequest, errMsg: String) = {
    val pipeline = addHeader("ACCESS-TOKEN", token) ~>
                   sendReceive ~>
                   unmarshal[HttpIdReply]
    val responseFuture = pipeline(req)
    responseFuture onComplete {
      case Success(ir) =>
        val newObjId = ir.id
      case Failure(error) =>
        log.info(error.toString() + errMsg)
    }
  }

  def updateData(putObj: AnyRef, nodeId: String, uri: String, errMsg: String) = {
    val getPipeline = addHeader("ACCESS-TOKEN", token) ~>
                      sendReceive ~>
                      unmarshal[HttpDataReply]
    val responseFuture = getPipeline(Get(uri + "/" + nodeId))
    responseFuture onComplete {
      case Success(ed) =>
        try {
          val res = decyptData(ed.data, ed.iv.get, ed.key.get)
          res._1 match {
            case true =>
              val json = parse(res._2)
              val value = Extraction.decompose(putObj)
              val updated = compact(render(json merge value))
              val iv = AES.generateIv()
              val encrypted = AES.encrypt(updated, ed.key.get, iv)
              val ud = UpdatedData(nodeId, encrypted, iv.getIV)
              val putPipeline = addHeader("ACCESS-TOKEN", token) ~> sendReceive
              putPipeline(Put(uri + "/" + nodeId, ud))
            case false => log.info(res._2)
          }
        } catch {
          case error: Throwable => log.info(error.toString() + "Decrypt failed")
        }
      case Failure(error) =>
        log.info(error.toString() + errMsg)
    }
  }

  def readFriendProfile() = {
    if (!myFriends.isEmpty) {
      val idx = Random.nextInt(myFriends.length)
      getUser(myFriends(idx))
    }
  }

  def getUser(userId: String) = {
    getData(userId,  Get(userUri + "/" + userId), s"Couldn't get user $userId")
  }

  def updateUser() = {
    val user = generateUpdate("user")
    updateData(user, id, userUri, s"Couldn't put user $id")
  }

  def deleteUser() = {
    val pipeline = addHeader("ACCESS-TOKEN", token) ~> sendReceive
    val responseFuture = pipeline { Delete(userUri + "/" + id) }
    stopHandler(responseFuture, s"Delete user $id failed")
  }

  def getPage(pageId: String) = {
    getData(pageId,  Get(pageUri + "/" + pageId), s"Couldn't get page $pageId")
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
    updateData(page, id, pageUri, s"Couldn't put page $id")
  }

  def deletePage() = {
    val pipeline = addHeader("ACCESS-TOKEN", token) ~> sendReceive
    val responseFuture = pipeline { Delete(pageUri + "/" + id) }
    stopHandler(responseFuture, s"Delete page $id failed")
  }

  def getNewFeeds() = {
    val pipeline = addHeader("ACCESS-TOKEN", token) ~>
                   sendReceive ~>
                   unmarshal[HttpListReply]
    val responseFuture = pipeline { Get(feedUri + "/pull") }
    responseFuture onComplete {
      case Success(lr) =>
        lr.list foreach {id => getFeed(id) }
      case Failure(error) =>
        log.info(error.toString(), "Couldn't delete feed")
    }
  }

  def getMyFeeds() = {
    val pipeline = addHeader("ACCESS-TOKEN", token) ~>
                   sendReceive ~>
                   unmarshal[HttpListReply]
    val responseFuture = pipeline { Get(feedUri + "/me") }
    responseFuture onComplete {
      case Success(lr) =>
        myPost = lr.list
      case Failure(error) =>
        log.info(error.toString(), "Couldn't delete feed")
    }
  }

  def getFeed(feedId: String) = {
    getData(feedId,  Get(feedUri + "/" + feedId), s"Couldn't get page $feedId")
  }

  def createFeed() = {
    val feed = generateCreate("feed")
    val jsonStr = Serialization.write(feed)
    val encrypted = encyptData(jsonStr)
    postData(Post(feedUri, encrypted), "Post feed failed")
  }

  def updateFeed() = {
    if (myPost.length > 0) {
      val feedId = myPost(Random.nextInt(myPost.length))
      val feed = generateUpdate("feed")
      updateData(feed, feedId, feedUri, s"Couldn't put feed $feedId")
    }
  }

  def deleteFeed() = {
    if (myPost.length > 0) {
      val feedId = myPost(Random.nextInt(myPost.length))
      val pipeline = addHeader("ACCESS-TOKEN", token) ~> sendReceive
      val responseFuture = pipeline { Delete(feedUri + "/" + feedId) }
      responseHandler(responseFuture, "Couldn't delete feed")
    }
  }

  def getFriends(friendListId: String) = {
    val pipeline = addHeader("ACCESS-TOKEN", token) ~>
                   sendReceive ~>
                   unmarshal[HttpListReply]
    val responseFuture = pipeline { Get(friendUri + "/" + friendListId + "/list") }
    responseFuture onComplete {
      case Success(lr) =>
        myFriends = lr.list
      case Failure(error) =>
        log.info(error.toString() +  "Couldn't get friend list")
    }
  }

  def getMyFriendLists = {
    val pipeline = addHeader("ACCESS-TOKEN", token) ~>
                   sendReceive ~>
                   unmarshal[HttpListReply]
    val responseFuture = pipeline { Get(friendUri + "/me") }
    responseFuture onComplete {
      case Success(lr) =>
        myFriendListIds = lr.list
      case Failure(error) =>
        log.info(error.toString() +  "Couldn't get friend list")
    }
  }

  def getFriendListInfo(friendListId: String) = {
    getData(friendListId,  Get(friendUri + "/" + friendListId + "/info"), s"Couldn't get page $friendListId")
  }

  def createFriends() = {
    val friends = generateCreate("friends")
    val jsonStr = Serialization.write(friends)
    val encrypted = encyptData(jsonStr)
    postData(Post(friendUri, encrypted), "Post friend list failed")
  }

  def updateFriends() = {
    if (!myFriends.isEmpty && !myFriendListIds.isEmpty) {
      val ids = myFriends.reduceLeft(_+","+_)
      val pipeline = addHeader("ACCESS-TOKEN", token) ~> sendReceive
      val responseFuture = pipeline { Put(friendUri + "/" + myFriendListIds(0) + "?ids=" + ids) }
      responseHandler(responseFuture, s"Put friends to friend list ${myFriendListIds(0)} failed")
    }
  }

  def deleteFriends() = {
    if (!myFriends.isEmpty) {
      val ids = myFriends.slice(0,2).reduceLeft(_+","+_)
      val pipeline = addHeader("ACCESS-TOKEN", token) ~> sendReceive
      val responseFuture = pipeline { Delete(friendUri + "/" + myFriendListIds(0) + "?ids=" + ids) }
      responseHandler(responseFuture, s"Delete friends to friend list ${myFriendListIds(0)} failed")
    }
  }

  def deleteFriendList() = {
    val pipeline = addHeader("ACCESS-TOKEN", token) ~> sendReceive
    val responseFuture = pipeline { Delete( friendUri + "/" + myFriendListIds(0)) }
    responseHandler(responseFuture, s"Delete friend list ${myFriendListIds(0)} failed")
  }

  def responseHandler(f: Future[HttpResponse], errMsg: String) = {
    f onComplete {
      case Success(res) =>
        println(res)
      case Failure(error) =>
        log.info(error.toString() + errMsg)
    }
  }

  def stopHandler(f: Future[HttpResponse], errMsg: String) = {
    f onComplete {
      case Success(res) =>
        context.stop(self)
      case Failure(error) =>
        log.error(error.toString() + errMsg)
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


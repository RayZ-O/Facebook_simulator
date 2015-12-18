package edu.ufl.dos15.client

import java.util.Base64
import java.io.BufferedInputStream
import java.io.FileInputStream
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
import java.security.KeyPair

class Client(id: String, host: String, port: Int, kp: KeyPair) extends Actor
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
  import edu.ufl.dos15.fbapi.FBMessage.RegisterUser
  import edu.ufl.dos15.fbapi.FBMessage.CheckNonce
  import edu.ufl.dos15.fbapi.FBMessage.Tick


  val baseUri = s"http://$host:$port"
  val userUri = baseUri + "/user"
  val pageUri = baseUri + "/page"
  val feedUri = baseUri + "/feed"
  val friendUri = baseUri + "/friends"

  val pubKey = kp.getPublic()
  private val priKey = kp.getPrivate()

  val bis = new BufferedInputStream(new FileInputStream("server.pem"))
  val bytes = Stream.continually(bis.read).takeWhile(-1 != _).map(_.toByte).toArray
  val serverPubKey = RSA.decodePubKey(bytes)

  import scala.collection.mutable.HashMap
  var friendsPubKeys = new HashMap[String, PublicKey]

  import scala.collection.mutable.ArrayBuffer
  var eToken = ""
  var myPost = new ArrayBuffer[String]
  var myFriends = new ArrayBuffer[String]
  var myFriendListIds = new ArrayBuffer[String]
  var tick: Cancellable = _

  def receive: Receive = {
    case Run(friends) =>
      login() match {
        case Some(token) =>
          val et = RSA.encrypt(token, serverPubKey)
          eToken = new String(Base64.getEncoder().encodeToString(et))
        case None => eToken = ""
      }
      val f = createFriends()
      friends foreach { friend =>
        if (friend._1 != id) {
          myFriends += friend._1
          friendsPubKeys += (friend._1 -> friend._2)
        }
      }
      f onComplete {
        case Success(rep) =>
          myFriendListIds += rep.id
          updateFriends()

        case Failure(error) =>
          log.error(error, "Couldn't post friend")
      }

    case Start => updateFriends()

    case Tick => takeAction()

    case _ =>
  }

  def takeAction() = {
    val n = Random.nextInt(100)
    if (n < 30) {
      getNewFeeds()
    } else if (n < 60) {
      createFeed()
    } else if (n < 80) {
      readProfile()
    } else if (n < 90) {
      updateMyFeeds()
    } else {
      updateUser()
    }
  }

  def getNonce(): Future[Option[String]] = {
    val pipeline = sendReceive ~> unmarshal[HttpDataReply]
    val responseFuture = pipeline( Get(baseUri + s"/login/$id") )
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
    val iv = AES.generateIv()
    val symKey = AES.generateKey()
    val encryptData = signedEncryptAES(data, priKey, symKey, iv, pubKey)
    val keyBytes = symKey.getEncoded()
    val keys = friendsPubKeys.map{ p => (p._1 -> RSA.encrypt(keyBytes, p._2)) }
    keys += id -> RSA.encrypt(keyBytes, pubKey)
    EncryptedData(encryptData, iv.getIV(), keys.toMap)
  }

  def decyptData(encryptedData: Array[Byte], encryptedKey: Array[Byte], iv: Array[Byte]) = {
    decryptAESVerify(encryptedData, encryptedKey, priKey, iv)
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
    val pipeline = addHeader("ACCESS-TOKEN", eToken) ~>
                   sendReceive ~>
                   unmarshal[HttpDataReply]
    val responseFuture = pipeline(req)
    val params = req.uri.query.get("fields")
    responseFuture onComplete {
      case Success(ed) =>
        try {
          val res = decyptData(ed.data, ed.key.get, ed.iv.get)
          res._1 match {
            case true =>
              val json = filterFields(nodeId, params, res._2)
              val jsonStr = compact(render(json))
              println("[GET] get data success" +jsonStr)
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
    val pipeline = addHeader("ACCESS-TOKEN", eToken) ~>
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
    val getPipeline = addHeader("ACCESS-TOKEN", eToken) ~>
                      sendReceive ~>
                      unmarshal[HttpDataReply]
    val responseFuture = getPipeline(Get(uri + "/" + nodeId))
    responseFuture onComplete {
      case Success(ed) =>
        try {
          val res = decyptData(ed.data, ed.key.get, ed.iv.get)
          res._1 match {
            case true =>
              val json = parse(res._2)
              val value = Extraction.decompose(putObj)
              val updated = compact(render(json merge value))
              val encrypted = encyptData(updated)
              println(s"[Put] update data ${new String(Base64.getEncoder().encodeToString(encrypted.data))}")
              val putPipeline = addHeader("ACCESS-TOKEN", eToken) ~> sendReceive
              putPipeline(Put(uri + "/" + nodeId, encrypted))
            case false => log.info(res._2)
          }
        } catch {
          case error: Throwable => log.info(error.toString() + "Decrypt failed")
        }
      case Failure(error) =>
        log.info(error.toString() + errMsg)
    }
  }

  def readProfile() = {
    getUser(id)
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
    val pipeline = addHeader("ACCESS-TOKEN", eToken) ~> sendReceive
    val responseFuture = pipeline { Delete(userUri + "/" + id) }
    stopHandler(responseFuture, s"Delete user $id failed")
  }

  def getPage(pageId: String) = {
    getData(pageId,  Get(pageUri + "/" + pageId), s"Couldn't get page $pageId")
  }

  def updatePage() = {
    val page = generateUpdate("page")
    updateData(page, id, pageUri, s"Couldn't put page $id")
  }

  def deletePage() = {
    val pipeline = addHeader("ACCESS-TOKEN", eToken) ~> sendReceive
    val responseFuture = pipeline { Delete(pageUri + "/" + id) }
    stopHandler(responseFuture, s"Delete page $id failed")
  }

  def getNewFeeds() = {
    val pipeline = addHeader("ACCESS-TOKEN", eToken) ~>
                   sendReceive ~>
                   unmarshal[HttpListReply]
    val responseFuture = pipeline { Get(feedUri + "/pull?start=0") }
    responseFuture onComplete {
      case Success(lr) =>
        lr.list foreach {id => getFeed(id) }
      case Failure(error) =>
        log.info(error.toString(), "Couldn't delete feed")
    }
  }

  def updateMyFeeds() = {
    val pipeline = addHeader("ACCESS-TOKEN", eToken) ~>
                   sendReceive ~>
                   unmarshal[HttpListReply]
    val responseFuture = pipeline { Get(feedUri + "/me") }
    responseFuture onComplete {
      case Success(lr) =>
        myPost = ArrayBuffer(lr.list : _*)
        updateFeed()
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
    println(s"[Post] Create data ${new String(Base64.getEncoder().encodeToString(encrypted.data))}")
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
      val pipeline = addHeader("ACCESS-TOKEN", eToken) ~> sendReceive
      val responseFuture = pipeline { Delete(feedUri + "/" + feedId) }
      responseHandler(responseFuture, "Couldn't delete feed")
    }
  }

  def getFriends(friendListId: String) = {
    val pipeline = addHeader("ACCESS-TOKEN", eToken) ~>
                   sendReceive ~>
                   unmarshal[HttpListReply]
    val responseFuture = pipeline { Get(friendUri + "/" + friendListId + "/list") }
    responseFuture onComplete {
      case Success(lr) =>
        myFriends = ArrayBuffer(lr.list : _*)
      case Failure(error) =>
        log.info(error.toString() +  "Couldn't get friend list")
    }
  }

  def getMyFriendLists = {
    val pipeline = addHeader("ACCESS-TOKEN", eToken) ~>
                   sendReceive ~>
                   unmarshal[HttpListReply]
    val responseFuture = pipeline { Get(friendUri + "/me") }
    responseFuture onComplete {
      case Success(lr) =>
        myFriendListIds = ArrayBuffer(lr.list : _*)
      case Failure(error) =>
        log.info(error.toString() +  "Couldn't get friend list")
    }
  }

  def getFriendListInfo(friendListId: String) = {
    getData(friendListId,  Get(friendUri + "/" + friendListId + "/info"), s"Couldn't get page $friendListId")
  }

  def createFriends(): Future[HttpIdReply] = {
    val friends = generateCreate("friends")
    val jsonStr = Serialization.write(friends)
    val encrypted = encyptData(jsonStr)
    val pipeline = addHeader("ACCESS-TOKEN", eToken) ~>
                   sendReceive ~>
                   unmarshal[HttpIdReply]
    pipeline{ Post(friendUri, encrypted) }
  }

  def updateFriends() = {
    if (!myFriends.isEmpty && !myFriendListIds.isEmpty) {
      val ids = myFriends.reduceLeft(_+","+_)
      val pipeline = addHeader("ACCESS-TOKEN", eToken) ~> sendReceive ~> unmarshal[HttpSuccessReply]
      val responseFuture = pipeline { Put(friendUri + "/" + myFriendListIds(0) + "?ids=" + ids) }
      responseFuture onComplete {
        case Success(rep) =>
          tick = context.system.scheduler.schedule(1.second, 1.second, self, Tick)
        case Failure(error) =>
          log.error(error, "Couldn't post friend")
      }
    }
  }

  def deleteFriends() = {
    if (!myFriends.isEmpty) {
      val ids = myFriends.slice(0,2).reduceLeft(_+","+_)
      val pipeline = addHeader("ACCESS-TOKEN", eToken) ~> sendReceive
      val responseFuture = pipeline { Delete(friendUri + "/" + myFriendListIds(0) + "?ids=" + ids) }
      responseHandler(responseFuture, s"Delete friends to friend list ${myFriendListIds(0)} failed")
    }
  }

  def deleteFriendList() = {
    val pipeline = addHeader("ACCESS-TOKEN", eToken) ~> sendReceive
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


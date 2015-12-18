package edu.ufl.dos15.fbapi

import java.util.Base64
import org.specs2.mutable.{Specification, Before}
import spray.testkit.Specs2RouteTest
import spray.http.StatusCodes._
import scala.concurrent.duration._
import akka.actor.{ActorSystem, Props}
import edu.ufl.dos15.db._
import edu.ufl.dos15.crypto.Crypto._
import org.json4s.native.JsonMethods._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class FriendListServiceSpec extends Specification with Specs2RouteTest with FriendListService with Before {
  import FriendListService._
  import FBMessage._

  implicit val routeTestTimeout = RouteTestTimeout(FiniteDuration(5, SECONDS))

  def actorRefFactory = system
  override val keyPair = RSA.generateKeyPair()

  val clienKeyPair = RSA.generateKeyPair()
  val priKey = clienKeyPair.getPrivate()
  val pubKey = clienKeyPair.getPublic()
  val id = "1"
  val token = "TOKEN"
  val etoken = RSA.encrypt(token, keyPair.getPublic())
  val etokenStr = new String(Base64.getEncoder().encodeToString(etoken))
  var friendListId = ""

  override def before() = {
    system.actorOf(Props[AuthDB], "auth-db")
    system.actorOf(Props[EncryptedDataDB], "data-db")
    system.actorOf(Props[FriendDB], "friend-db")
    system.actorOf(Props[PubSubDB], "pub-sub-db")
  }

  def initialize() = {
    actorRefFactory.actorSelection("/user/auth-db") ! DBTestToken(id, token)
    val data = """{"first_name": "Rui",
                   "last_name": "Zhang"}"""
    val secKey = AES.generateKey()
    val iv = AES.generateIv()
    val edata = signedEncryptAES(data, priKey, secKey, iv, pubKey)
    val encryptedKey = RSA.encrypt(secKey.getEncoded(), pubKey)
    actorRefFactory.actorSelection("/user/data-db") ! DBTestInsert(id, edata)
    actorRefFactory.actorSelection("/user/pub-sub-db") ! CreateChannel(id, iv.getIV(), encryptedKey)
  }

  sequential

  "The FriendListService" should {

    "return OK for GET requests to /friends" in {
      Get("/friends") ~> friendListRoute ~> check {
        response.status should be equalTo OK
      }
    }

    "return id for POST requests to /friend" in {
      initialize()
      val data = """{"name": "good friend"}"""
      val secKey = AES.generateKey()
      val iv = AES.generateIv()
      val encryptedKey = RSA.encrypt(secKey.getEncoded(), pubKey)
      val edata = signedEncryptAES(data, priKey, secKey, iv, pubKey)
      Post("/friends", EncryptedData(edata, iv.getIV(), Map(id->encryptedKey))) ~>
          addHeader("ACCESS-TOKEN", etokenStr) ~> friendListRoute ~> check {
        response.status should be equalTo Created
        response.entity should not be equalTo(None)
        val reply = responseAs[HttpIdReply]
        friendListId = reply.id
        reply.id.equals("") should be equalTo(false)
      }
    }

    "return friend list infomation for GET request to /friends/{id}/info" in {
      Get(s"/friends/$friendListId/info") ~> addHeader("ACCESS-TOKEN", etokenStr) ~> friendListRoute ~> check {
        response.status should be equalTo OK
        response.entity should not be equalTo(None)
        val reply = responseAs[HttpDataReply]
        val res = decryptAESVerify(reply.data, reply.key.get, priKey, reply.iv.get)
        res._1 should be equalTo(true)
        val friends = parse(res._2).extract[FriendList]
        friends === FriendList(name=Some("good friend"))
      }
    }

    "return my friend list id for GET request to /friends/me" in {
      Get(s"/friends/me") ~> addHeader("ACCESS-TOKEN", etokenStr) ~> friendListRoute ~> check {
        response.status should be equalTo OK
        response.entity should not be equalTo(None)
        val reply = responseAs[HttpListReply]
        reply.list(0) === friendListId
      }
    }

    "return success for PUT request to existed id with entity" in {
      val data = """{"list_type": 0}"""
      val secKey = AES.generateKey()
      val iv = AES.generateIv()
      val encryptedKey = RSA.encrypt(secKey.getEncoded(), pubKey)
      val edata = signedEncryptAES(data, priKey, secKey, iv, pubKey)
      Put(s"/friends/$friendListId", EncryptedData(edata, iv.getIV(), Map(id->encryptedKey))) ~>
          addHeader("ACCESS-TOKEN", etokenStr) ~> friendListRoute ~> check {
        response.status should be equalTo OK
        response.entity should not be equalTo(None)
        val reply = responseAs[HttpSuccessReply]
        reply.success should be equalTo(true)
      }
    }

    "return success for PUT request to existed id with parameter" in {
      Put(s"/friends/$friendListId?ids=1234,2345,3456") ~> addHeader("ACCESS-TOKEN", etokenStr) ~>
          friendListRoute ~> check {
        response.status should be equalTo OK
        response.entity should not be equalTo(None)
        val reply = responseAs[HttpSuccessReply]
        reply.success should be equalTo(true)
      }
    }

    "return ids in friend list for GET request to /friends/{id}/list"  in {
      Get(s"/friends/$friendListId/list") ~> addHeader("ACCESS-TOKEN", etokenStr) ~> friendListRoute ~> check {
        response.status should be equalTo OK
        response.entity should not be equalTo(None)
        val reply = responseAs[HttpListReply]
        val fl = reply.list
        fl.contains("1234") should be equalTo(true)
        fl.contains("2345") should be equalTo(true)
        fl.contains("3456") should be equalTo(true)
      }
    }

    "return success for DELETE request to existed id with parameter" in {
      Delete(s"/friends/$friendListId?ids=1234,2345,3456")  ~> addHeader("ACCESS-TOKEN", etokenStr) ~>
          friendListRoute ~> check {
        response.status should be equalTo OK
        response.entity should not be equalTo(None)
        val reply = responseAs[HttpSuccessReply]
        reply.success should be equalTo(true)
      }
    }

    "return success for DELETE request to existed id" in {
      Delete(s"/friends/$friendListId") ~> addHeader("ACCESS-TOKEN", etokenStr) ~> friendListRoute ~> check {
        response.status should be equalTo OK
        response.entity should not be equalTo(None)
        val reply = responseAs[HttpSuccessReply]
        reply.success should be equalTo(true)
      }
    }

    "return NotFound for GET request to non-existed id" in {
      Get("/friends/2/info") ~> addHeader("ACCESS-TOKEN", etokenStr) ~> friendListRoute ~> check {
        response.status should be equalTo NotFound
      }
    }

    "return NotFound for PUT request to non-existed id" in {
      Put(s"/friends/2?ids=1234,2345,3456") ~> addHeader("ACCESS-TOKEN", etokenStr) ~>
          friendListRoute ~> check {
        response.status should be equalTo NotFound
      }
    }

    "return NotFound for DELETE request to non-existed id" in {
      Delete("/friends/2") ~> addHeader("ACCESS-TOKEN", etokenStr) ~> friendListRoute ~> check {
        response.status should be equalTo NotFound
      }
    }
  }
}

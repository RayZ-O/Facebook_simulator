package edu.ufl.dos15.fbapi

import java.util.Base64
import java.security.PrivateKey
import java.security.PublicKey
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
class FeedServiceSpec extends Specification with Specs2RouteTest with FeedService with Before {
  import FeedService._
  import FBMessage._

  implicit val routeTestTimeout = RouteTestTimeout(FiniteDuration(5, SECONDS))

  def actorRefFactory = system
  override val keyPair = RSA.generateKeyPair()
  // user1
  val clienKeyPair1 = RSA.generateKeyPair()
  val priKey1 = clienKeyPair1.getPrivate()
  val pubKey1 = clienKeyPair1.getPublic()
  val id1 = "1"
  val token1 = "TOKEN1"
  val etoken1 = RSA.encrypt(token1, keyPair.getPublic())
  val etokenStr1 = new String(Base64.getEncoder().encodeToString(etoken1))
  // user2
  val clienKeyPair2 = RSA.generateKeyPair()
  val priKey2 = clienKeyPair1.getPrivate()
  val pubKey2 = clienKeyPair1.getPublic()
  val id2 = "21"
  val token2 = "TOKEN2"
  val etoken2 = RSA.encrypt(token2, keyPair.getPublic())
  val etokenStr2 = new String(Base64.getEncoder().encodeToString(etoken2))

  var feedId = ""

  override def before() = {
    system.actorOf(Props[AuthDB], "auth-db")
    system.actorOf(Props[EncryptedDataDB], "data-db")
    system.actorOf(Props[PubSubDB], "pub-sub-db")
  }

  def initialize() = {
     val data = """{"first_name": "Rui",
                    "last_name": "Zhang"}"""
     createUser(id1, token1, data, priKey1, pubKey1)
     createUser(id2, token2, data, priKey2, pubKey2)
  }

  def createUser(id: String, token: String, data: String, priKey: PrivateKey, pubKey: PublicKey) = {
    actorRefFactory.actorSelection("/user/auth-db") ! DBTestToken(id, token)
    val secKey = AES.generateKey()
    val iv = AES.generateIv()
    val edata = signedEncryptAES(data, priKey, secKey, iv, pubKey)
    val encryptedKey = RSA.encrypt(secKey.getEncoded(), pubKey)
    actorRefFactory.actorSelection("/user/data-db") ! DBTestInsert(id, edata)
    actorRefFactory.actorSelection("/user/pub-sub-db") ! CreateChannel(id, iv.getIV(), encryptedKey)
  }

  sequential

  "The FeedService" should {

    "return OK for GET requests to /feed" in {
      Get("/feed") ~> feedRoute ~> check {
        response.status should be equalTo OK
      }
    }

    "return id for POST requests to /feed" in {
      initialize()
      val data = """{"message": "I am happy"}"""
      val secKey = AES.generateKey()
      val iv = AES.generateIv()
      val eKey1 = RSA.encrypt(secKey.getEncoded(), pubKey1)
      val eKey2 = RSA.encrypt(secKey.getEncoded(), pubKey2)
      val edata = signedEncryptAES(data, priKey1, secKey, iv, pubKey1)
      Post("/feed", EncryptedData(edata, iv.getIV(), Map(id1->eKey1, id2->eKey2))) ~>
          addHeader("ACCESS-TOKEN", etokenStr1) ~> feedRoute ~> check {
        response.status should be equalTo Created
        response.entity should not be equalTo(None)
        val reply = responseAs[HttpIdReply]
        feedId = reply.id
        feedId.equals("") should be equalTo(false)
      }
    }

    "return all fileds for GET request to /feed/{id}" in {
      Get(s"/feed/$feedId") ~> addHeader("ACCESS-TOKEN", etokenStr1) ~> feedRoute ~> check {
        response.status should be equalTo OK
        response.entity should not be equalTo(None)
        val reply = responseAs[HttpDataReply]
        val res = decryptAESVerify(reply.data, reply.key.get, priKey1, reply.iv.get)
        res._1 should be equalTo(true)
        val feed = parse(res._2).extract[Feed]
        feed === Feed(message=Some("I am happy"))
      }
    }

    "return my posts for GET request to /feed/me" in {
      Get("/feed/me") ~> addHeader("ACCESS-TOKEN", etokenStr1) ~> feedRoute ~> check {
        response.status should be equalTo OK
        response.entity should not be equalTo(None)
        val reply = responseAs[HttpListReply]
        reply.list(0) === feedId
      }
    }

    "return friend's posts for GET request to /feed/pull" in {
      Get("/feed/pull?start=0") ~> addHeader("ACCESS-TOKEN", etokenStr2) ~> feedRoute ~> check {
        response.status should be equalTo OK
        response.entity should not be equalTo(None)
        val reply = responseAs[HttpListReply]
        reply.list(0) === feedId
      }
    }

    "return all fileds for GET request to /feed/{friend's-feed-id}" in {
      Get(s"/feed/$feedId") ~> addHeader("ACCESS-TOKEN", etokenStr2) ~> feedRoute ~> check {
        response.status should be equalTo OK
        response.entity should not be equalTo(None)
        val reply = responseAs[HttpDataReply]
        val res = decryptAESVerify(reply.data, reply.key.get, priKey1, reply.iv.get)
        res._1 should be equalTo(true)
        val feed = parse(res._2).extract[Feed]
        feed === Feed(message=Some("I am happy"))
      }
    }

    "return success for PUT request to existed id" in {
      val data = """{"message": "I am happy yesterday"}"""
      val secKey = AES.generateKey()
      val iv = AES.generateIv()
      val encryptedKey = RSA.encrypt(secKey.getEncoded(), pubKey1)
      val edata = signedEncryptAES(data, priKey1, secKey, iv, pubKey1)
      Put(s"/feed/$feedId", EncryptedData(edata, iv.getIV(), Map(id1->encryptedKey))) ~>
          addHeader("ACCESS-TOKEN", etokenStr1) ~> feedRoute ~> check {
        response.status should be equalTo OK
        response.entity should not be equalTo(None)
        val reply = responseAs[HttpSuccessReply]
        reply.success should be equalTo(true)
      }
    }

    "return success for DELETE request to existed id" in {
      Delete(s"/feed/$feedId") ~> addHeader("ACCESS-TOKEN", etokenStr1) ~> feedRoute ~> check {
        response.status should be equalTo OK
        response.entity should not be equalTo(None)
        val reply = responseAs[HttpSuccessReply]
        reply.success should be equalTo(true)
      }
    }

    "return NotFound for GET request to non-existed id" in {
      Get("/feed/2")  ~> addHeader("ACCESS-TOKEN", etokenStr1) ~> feedRoute ~> check {
        response.status should be equalTo NotFound
      }
    }

    "return NotFound for PUT request to non-existed id" in {
      val data = """{"message": "I am happy yesterday"}"""
      val secKey = AES.generateKey()
      val iv = AES.generateIv()
      val encryptedKey = RSA.encrypt(secKey.getEncoded(), pubKey1)
      val edata = signedEncryptAES(data, priKey1, secKey, iv, pubKey1)
      Put("/feed/2", EncryptedData(edata, iv.getIV(), Map(id1->encryptedKey))) ~>
          addHeader("ACCESS-TOKEN", etokenStr1) ~> feedRoute ~> check {
        response.status should be equalTo NotFound
      }
    }

    "return NotFound for DELETE request to non-existed id" in {
      Delete("/feed/2") ~> addHeader("ACCESS-TOKEN", etokenStr1) ~> feedRoute ~> check {
        response.status should be equalTo NotFound
      }
    }
  }
}

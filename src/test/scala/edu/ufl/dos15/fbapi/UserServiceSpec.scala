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
class UserServiceSpec extends Specification with Specs2RouteTest with UserService with Before {
  import UserService._
  import FBMessage._

  implicit val routeTestTimeout = RouteTestTimeout(FiniteDuration(5, SECONDS))

  override def actorRefFactory = system
  override val keyPair = RSA.generateKeyPair()

  val clienKeyPair = RSA.generateKeyPair()
  val priKey = clienKeyPair.getPrivate()
  val pubKey = clienKeyPair.getPublic()
  val id = "1"
  val token = "TOKEN"
  val etoken = RSA.encrypt(token, keyPair.getPublic())
  val etokenStr = new String(Base64.getEncoder().encodeToString(etoken))

  override def before() = {
    system.actorOf(Props[AuthDB], "auth-db")
    system.actorOf(Props[EncryptedDataDB], "data-db")
    system.actorOf(Props[PubSubDB], "pub-sub-db")
  }

  def initialize() = {
    actorRefFactory.actorSelection("/user/auth-db") ! DBTestToken(id, token)
    val data = """{"email": "ruizhang1011@ufl.edu",
                   "gender": "male",
                   "first_name": "Rui",
                   "last_name": "Zhang"}"""
    val secKey = AES.generateKey()
    val iv = AES.generateIv()
    val edata = signedEncryptAES(data, priKey, secKey, iv, pubKey)
    val encryptedKey = RSA.encrypt(secKey.getEncoded(), pubKey)
    actorRefFactory.actorSelection("/user/data-db") ! DBTestInsert(id, edata)
    actorRefFactory.actorSelection("/user/pub-sub-db") ! CreateChannel(id, iv.getIV(), encryptedKey)
  }

  sequential

  "The UserService" should {

    "return OK for GET requests to /user" in {
      Get("/user") ~> userRoute ~> check {
        response.status should be equalTo OK
      }
    }

    "return all fileds for GET request to /user/{id}" in {
      initialize() // add a user
      Get(s"/user/$id") ~> addHeader("ACCESS-TOKEN", etokenStr) ~> userRoute ~> check {
        response.status should be equalTo OK
        response.entity should not be equalTo(None)
        val reply = responseAs[HttpDataReply]
        val res = decryptAESVerify(reply.data, reply.key.get, priKey, reply.iv.get)
        res._1 should be equalTo(true)
        val user = parse(res._2).extract[User]
        user === User(email=Some("ruizhang1011@ufl.edu"),
                      gender=Some("male"),
                      first_name=Some("Rui"),
                      last_name=Some("Zhang"))
      }
    }

    "return success for PUT request to existed id" in {
      val data = """{"email": "ruizhang1011@ufl.edu"}"""
      val secKey = AES.generateKey()
      val iv = AES.generateIv()
      val encryptedKey = RSA.encrypt(secKey.getEncoded(), pubKey)
      val edata = signedEncryptAES(data, priKey, secKey, iv, pubKey)
      Put(s"/user/$id", EncryptedData(edata, iv.getIV(), Map(id->encryptedKey))) ~>
          addHeader("ACCESS-TOKEN", etokenStr) ~> userRoute ~> check {
        response.status should be equalTo OK
        response.entity should not be equalTo(None)
        val reply = responseAs[HttpSuccessReply]
        reply.success should be equalTo(true)
      }
    }

    "return success for DELETE request to existed id" in {
      Delete(s"/user/$id") ~> addHeader("ACCESS-TOKEN", etokenStr) ~> userRoute ~> check {
        response.status should be equalTo OK
        response.entity should not be equalTo(None)
        val reply = responseAs[HttpSuccessReply]
        reply.success should be equalTo(true)
      }
    }

    "return NotFound for GET request to non-existed id" in {
      Get("/user/2") ~> addHeader("ACCESS-TOKEN", etokenStr)  ~> userRoute ~> check {
        response.status should be equalTo NotFound
      }
    }

    "return NotFound for PUT request to non-existed id" in {
      val data = """{"first_name": "Sanfeng"}"""
      val secKey = AES.generateKey()
      val iv = AES.generateIv()
      val encryptedKey = RSA.encrypt(secKey.getEncoded(), pubKey)
      val edata = signedEncryptAES(data, priKey, secKey, iv, pubKey)
      Put("/user/2", EncryptedData(edata, iv.getIV(), Map(id->encryptedKey))) ~>
          addHeader("ACCESS-TOKEN", etokenStr) ~> userRoute ~> check {
        response.status should be equalTo NotFound
      }
    }

    "return NotFound for DELETE request to non-existed id" in {
      Delete("/user/2") ~> addHeader("ACCESS-TOKEN", etokenStr) ~> userRoute ~> check {
        response.status should be equalTo NotFound
      }
    }
  }
}

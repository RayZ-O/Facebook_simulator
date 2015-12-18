package edu.ufl.dos15.fbapi

import org.specs2.mutable.{Specification, Before}
import spray.testkit.Specs2RouteTest
import spray.http.StatusCodes._
import scala.concurrent.duration._
import akka.actor.{ActorSystem, Props}
import com.roundeights.hasher.Implicits._
import edu.ufl.dos15.db._
import edu.ufl.dos15.crypto.Crypto._
import java.util.Base64
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class AuthServiceSpec extends Specification with Specs2RouteTest with AuthService
    with UserService with Before {
  import FeedService._
  import FBMessage._

  implicit val routeTestTimeout = RouteTestTimeout(FiniteDuration(5, SECONDS))

  override def actorRefFactory = system
  override val keyPair = RSA.generateKeyPair()

  val clienKeyPair = RSA.generateKeyPair()
  var id = ""
  var nonce = ""
  var token = ""

  override def before() = {
    system.actorOf(Props[AuthDB], "auth-db")
    system.actorOf(Props[EncryptedDataDB], "data-db")
    system.actorOf(Props[PubSubDB], "pub-sub-db")
  }

  sequential

  "The AuthService" should {
    "return Id for register requests" in {
      val data = """{"email": "ruizhang1011@ufl.edu",
                     "gender": "male",
                     "first_name": "Rui",
                     "last_name": "Zhang"}"""
      val iv = AES.generateIv()
      val symKey = AES.generateKey()
      val encrypted = signedEncryptAES(data, clienKeyPair.getPrivate(), symKey, iv, clienKeyPair.getPublic())
      val ekey = RSA.encrypt(symKey.getEncoded(), clienKeyPair.getPublic().getEncoded())
      val reg = RegisterUser(encrypted, iv.getIV,ekey, clienKeyPair.getPublic().getEncoded())
      Post("/register", reg) ~> authRoute ~> check {
        response.status should be equalTo Created
        response.entity should not be equalTo(None)
        val reply = responseAs[HttpIdReply]
        id = reply.id
        id.equals("") should be equalTo(false)
      }
    }

    "return nonce for get requests to /login/{id}" in {
      Get(s"/login/$id") ~> authRoute ~> check {
        response.status should be equalTo OK
        response.entity should not be equalTo(None)
        val reply = responseAs[HttpDataReply]
        nonce = new String(RSA.decrypt(reply.data, clienKeyPair.getPrivate()))
        nonce.equals("") should be equalTo(false)
      }
    }

    "return token for correct credential to /login/pubkey" in {
      val sign = RSA.sign(nonce, clienKeyPair.getPrivate())
      val cn = CheckNonce(nonce, sign)
      Post("/login/pubkey", cn) ~> authRoute ~> check {
        response.status should be equalTo OK
        response.entity should not be equalTo(None)
        val reply = responseAs[HttpDataReply]
        token = new String(RSA.decrypt(reply.data, clienKeyPair.getPrivate()))
        token.equals("") should be equalTo(false)
      }
    }

    "authenticatie successfully for valid token" in {
      val etoken = RSA.encrypt(token, keyPair.getPublic())
      val etokenStr = new String(Base64.getEncoder().encodeToString(etoken))
      Get(s"/user/$id") ~> addHeader("ACCESS-TOKEN", etokenStr)~> userRoute ~> check {
        response.status should be equalTo OK
      }
    }
  }
}

package edu.ufl.dos15.fbapi

import org.specs2.mutable.{Specification, Before}
import spray.testkit.Specs2RouteTest
import spray.http.StatusCodes._
import scala.concurrent.duration._
import akka.actor.{ActorSystem, Props}
import com.roundeights.hasher.Implicits._
import edu.ufl.dos15.db._
import edu.ufl.dos15.crypto.Crypto._

class AuthServiceSpec extends Specification with Specs2RouteTest with AuthService with Before {
  import FeedService._
  import FBMessage._

  implicit val routeTestTimeout = RouteTestTimeout(FiniteDuration(5, SECONDS))

  override def actorRefFactory = system
  override val keyPair = RSA.generateKeyPair()

  val clienKeyPair = RSA.generateKeyPair()
  val name = "ruizhang"
  val password = "password"
  var id = ""
  var nonce = ""
  var token = ""

  def before() = {
    val db = system.actorOf(Props[AuthDB], "auth-db")
  }

  sequential

  "The AuthService" should {
    "return Id for register requests" in {
      val cred = RSA.encrypt(name + "|" + password.bcrypt.hex, keyPair.getPublic().getEncoded())
      Post("/register", RegisterCred(cred, clienKeyPair.getPublic().getEncoded())) ~> authRoute ~> check {
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
  }
}

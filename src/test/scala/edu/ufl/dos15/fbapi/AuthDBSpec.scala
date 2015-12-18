package edu.ufl.dos15.fbapi

import scala.concurrent.duration._
import akka.actor.{ActorSystem, Props, ActorRef}
import akka.testkit.TestKit
import akka.testkit.ImplicitSender
import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterAll
import com.roundeights.hasher.Implicits._
import edu.ufl.dos15.db._
import edu.ufl.dos15.crypto.Crypto._
import edu.ufl.dos15.fbapi.FBMessage._

class AuthDBSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("AuthDBSpec"))

  val kPair = RSA.generateKeyPair()
  var authDB: ActorRef = _
  var userId = ""
  var nonce = ""
  var token =""

  override def beforeAll {
    authDB = system.actorOf(Props[AuthDB], "auth-db")
  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "An AuthDB" must {
    "send back true and id for register" in {
      val pub = kPair.getPublic().getEncoded()
      authDB ! Register(pub)
      expectMsgPF() {
        case DBStrReply(succ, id, key) if (succ == true && id.isDefined) =>
          userId = id.get
          true
        case _ => false
      } should be(true)
    }

    "send back nonce" in {
      authDB ! GetNonce(userId)
      expectMsgPF() {
        case DBStrReply(succ, n, key) if (succ == true && n.isDefined) =>
          nonce = n.get
          true
        case _ => false
      } should be(true)
    }

    "send back false non-exist nonce" in {
      val sign = RSA.sign(nonce+"1", kPair.getPrivate())
      authDB ! CheckNonce(nonce+"1", sign)
      expectMsg(DBStrReply(false))
    }

    "send back token for valid nonce and digital signature" in {
      val sign = RSA.sign(nonce, kPair.getPrivate())
      authDB ! CheckNonce(nonce, sign)
      expectMsgPF() {
        case DBStrReply(succ, t, key) if (succ == true && t.isDefined) =>
          token = t.get
          true
        case _ => false
      } should be(true)
    }

    "send back false for wrong digital signature" in {
      authDB ! GetNonce(userId)
      val reply = expectMsgClass(classOf[DBStrReply])
      reply.success should be(true)
      nonce = reply.content.get
      val priKey = RSA.generateKeyPair().getPrivate()
      val sign = RSA.sign(nonce, priKey)
      authDB ! CheckNonce(nonce, sign)
      expectMsg(DBStrReply(false))
    }

    "send back false for used nonce even if signture is correct" in {
      val sign = RSA.sign(nonce, kPair.getPrivate())
      authDB ! CheckNonce(nonce, sign)
      expectMsg(DBStrReply(false))
    }

    "send back true and id for valid token" in {
      authDB ! TokenAuth(token)
      val reply = expectMsgClass(classOf[DBStrReply])
      reply.success should be(true)
      reply.content.getOrElse("") should be(userId)
    }
  }
}

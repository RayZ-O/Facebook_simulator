package edu.ufl.dos15.db

import akka.actor.{Actor, ActorLogging}
import scala.concurrent.duration._
import java.util.UUID
import akka.actor.actorRef2Scala
import edu.ufl.dos15.fbapi.FBMessage._
import edu.ufl.dos15.crypto._

case class Credentials(passwd: String, pubKey: String)
case class NonceInfo(id: String, expireOn: Long)
case class TokenInfo(id: String, expireOn: Long)

class AuthDB extends Actor with ActorLogging {
  import scala.collection.mutable.HashMap
  private var nameDB = new HashMap[String, String]
  private var credDB = new HashMap[String, Credentials]
  import scala.collection.mutable.LinkedHashMap
  private var nonceDB = new LinkedHashMap[String, NonceInfo]
  private var tokenDB = new LinkedHashMap[String, TokenInfo]

  import context.dispatcher
  val tick = context.system.scheduler.schedule(1.second, 60.second, self, Tick)
  var sequenceNum = 0

  def receive = {
    case Register(name, passwd, pub) =>
      if (nameDB.contains(name)) {
        sender ! DBReply(false)
      } else {
        val id = generateId()
        nameDB += (name -> id)
        credDB += (id -> Credentials(passwd, pub))
        sender ! DBReply(true)
      }

    case GetNonce(id) =>
      val nounce = Crypto.generateNonce(96)
      val expire = System.currentTimeMillis + 300000L
      nonceDB += (nounce -> NonceInfo(id, expire))
      sender ! DBReply(true, Some(nounce))

    case PassWdAuth(name, passwd) =>
      val id = nameDB.getOrElse(name, "")
      credDB.get(id) match {
        case Some(cred) =>
          if (cred.passwd == passwd) {
            val token = Crypto.generateToken(96)
            val expire = System.currentTimeMillis + 3600000L
            tokenDB += (token -> TokenInfo(id, expire))
            sender ! DBReply(true, Some(token))
          } else {
            sender ! DBReply(false)
          }
        case None => sender ! DBReply(false)
      }

    case TokenAuth(token) =>
      tokenDB.get(token) match {
        case Some(ti) => sender ! DBReply(true, Some(ti.id))
        case None => sender ! DBReply(false)
      }

    case Tick =>
      nonceDB.dropWhile(t => t._2.expireOn < System.currentTimeMillis())
      tokenDB.dropWhile(t => t._2.expireOn < System.currentTimeMillis())
  }

  def generateId() = {
    sequenceNum += 1
    System.currentTimeMillis().toString + sequenceNum
  }
}

package edu.ufl.dos15.db

import akka.actor.{Actor, ActorLogging}
import scala.concurrent.duration._
import java.util.UUID
import akka.actor.actorRef2Scala
import edu.ufl.dos15.fbapi.PassWdAuth
import edu.ufl.dos15.fbapi.Register
import edu.ufl.dos15.fbapi.Tick
import edu.ufl.dos15.fbapi.TokenAuth
import scala.collection.mutable.HashMap
import scala.collection.mutable.LinkedHashMap
import edu.ufl.dos15.fbapi.DBReply

case class Credentials(passwd: String, id: String, pubKey: String)
case class TokenInfo(id: String, expireOn: Long)

class AuthDB extends Actor with ActorLogging {
  import scala.collection.mutable.HashMap
  private var credDB = new HashMap[String, Credentials]
  import scala.collection.mutable.LinkedHashMap
  private var tokenDB = new LinkedHashMap[String, TokenInfo]

  import context.dispatcher
  val tick = context.system.scheduler.schedule(1.second, 1.second, self, Tick)
  var sequenceNum = 0

  def receive = {
    case Register(name, passwd, pub) =>
      if (credDB.contains(name)) {
        sender ! DBReply(false)
      } else {
        val id = generateId()
        credDB += (name -> Credentials(passwd, id, pub))
        sender ! DBReply(true)
      }

    case PassWdAuth(name, passwd) =>
       credDB.get(name) match {
         case Some(cred) =>
           if (cred.passwd == passwd) {
             val token = generateToken()
             val expire = System.currentTimeMillis + 3600000L
             tokenDB += (token -> TokenInfo(cred.id, expire))
             sender ! DBReply(true, Some(token))
           } else {
             sender ! DBReply(false)
           }
         case None => sender ! DBReply(false)
       }

    case TokenAuth(token) =>
      tokenDB.get(token) match {
        case Some(ti) => sender ! DBReply(true)
        case None => sender ! DBReply(false)
      }

    case Tick =>
      tokenDB.dropWhile(t => t._2.expireOn < System.currentTimeMillis())
  }

  def generateToken() = {
    UUID.randomUUID().toString().toUpperCase()
  }

  def generateId() = {
    sequenceNum += 1
    System.currentTimeMillis().toString + sequenceNum
  }
}

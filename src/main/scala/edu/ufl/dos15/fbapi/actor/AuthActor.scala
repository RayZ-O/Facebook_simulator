package edu.ufl.dos15.fbapi.actor

import akka.actor.{Actor, ActorLogging, Props}
import spray.routing.{RequestContext, HttpService}
import com.roundeights.hasher.Implicits._
import spray.http.StatusCodes
import edu.ufl.dos15.fbapi.Json4sProtocol
import edu.ufl.dos15.fbapi.FBMessage._
import edu.ufl.dos15.crypto._

class AuthActor(reqctx: RequestContext, message: Message) extends Actor
    with ActorLogging with Json4sProtocol with RequestHandler {
  val db = context.actorSelection("/user/auth-db")
  val ctx = reqctx
  var userId = ""
  var pubKey: Array[Byte] = _

  message match {
    case RegisterUser(data, iv, key, pub) =>
      pubKey = pub
      context.become(timeoutBehaviour orElse waitingRegister)
      sendToDB(Register(pub))

    case gn: GetNonce =>
      context.become(timeoutBehaviour orElse waitingNonce)
      sendToDB(gn)

    case cn: CheckNonce =>
      context.become(timeoutBehaviour orElse waitingToken)
      sendToDB(cn)

    case msg =>
      throw new UnsupportedOperationException(s"Unsupported Operation $msg in auth actor")
  }

  def waitingRegister: Receive = {
    case DBStrReply(succ, id, key) =>
      succ match {
        case true =>
          userId = id.get
          val dataDB = context.actorSelection("/user/data-db")
          context.become(timeoutBehaviour orElse waitingInsertData)
          val ru = message.asInstanceOf[RegisterUser]
          dataDB ! InsertNew(id.get, ru.data)
        case false => complete(StatusCodes.BadRequest, Error("Register failed"))
      }
  }

  def waitingInsertData: Receive = {
     case DBSuccessReply(succ) =>
       succ match {
        case true =>
          val pubSubDB = context.actorSelection("/user/pub-sub-db")
          context.become(timeoutBehaviour orElse waitingPublish)
          val ru = message.asInstanceOf[RegisterUser]
          pubSubDB ! CreateChannel(userId, ru.iv, ru.key)
        case false => complete(StatusCodes.BadRequest, Error("Register insert failed"))
      }
  }

  def waitingPublish: Receive = {
    case DBSuccessReply(succ) =>
      succ match {
        case true => complete(StatusCodes.Created, HttpIdReply(userId))
        case false => complete(StatusCodes.BadRequest, Error("Register publish failed"))
      }
  }

  def waitingNonce: Receive = {
    case DBStrReply(succ, nonce, key) =>
      succ match {
        case true =>
          val encrypted = Crypto.RSA.encrypt(nonce.get, key.get)
          complete(StatusCodes.OK, HttpDataReply(encrypted))
        case false => complete(StatusCodes.BadRequest, Error("id not exist"))
      }
  }

  def waitingToken: Receive = {
    case DBStrReply(succ, token, key) =>
      succ match {
        case true =>
          val encrypted = Crypto.RSA.encrypt(token.get, key.get)
          complete(StatusCodes.OK, HttpDataReply(encrypted))
        case false => complete(StatusCodes.Unauthorized, Error("Improper authentication credentials"))
      }
  }
}


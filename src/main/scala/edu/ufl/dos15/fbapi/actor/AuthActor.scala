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
  val db = context.actorSelection("/user/authdb")
  val ctx = reqctx
  val namePattern = "^[a-z0-9_-]{3,15}$".r

  message match {
    case Register(name, passwd, pub) =>
      name match {
        case namePattern() =>
          context.become(timeoutBehaviour orElse waitingRegister)
          sendToDB(Register(name, repeatedHash(3, passwd), pub))
        case _ =>
          complete(StatusCodes.BadRequest, Error("illegal username"))
      }

    case gn: GetNonce =>
      context.become(timeoutBehaviour orElse waitingNonce)
      sendToDB(gn)

    case pwa: PassWdAuth =>
      context.become(timeoutBehaviour orElse waitingToken)
      sendToDB(pwa)

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
          val encrypted = Crypto.RSA.encrypt(id.get, key.get)
          complete(StatusCodes.OK, HttpDataReply(encrypted))
        case false => complete(StatusCodes.BadRequest, Error("username has already been taken"))
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

  def repeatedHash(n: Int, text: String): String = {
    if (n <= 0) text
    else repeatedHash(n - 1, text.sha256.hex)
  }
}


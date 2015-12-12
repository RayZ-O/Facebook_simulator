package edu.ufl.dos15.fbapi.actor

import akka.actor.{Actor, ActorSelection, ReceiveTimeout, OneForOneStrategy}
import akka.actor.SupervisorStrategy.Stop
import spray.routing.RequestContext
import spray.http.{StatusCode, StatusCodes}
import scala.concurrent.duration._
import edu.ufl.dos15.fbapi.Json4sProtocol
import edu.ufl.dos15.fbapi.FBMessage._

trait RequestHandler {
  this: Actor with Json4sProtocol =>
  val db: ActorSelection
  val ctx: RequestContext

  override val supervisorStrategy =
    OneForOneStrategy() {
      case e => {
        complete(StatusCodes.InternalServerError, e.getMessage)
        Stop
      }
  }

  val defaultMatcher: PartialFunction[Any, Unit] = {
    case f: Fetch =>
      sendToDB(f)

    case i: Insert =>
      sendToDB(i)

    case u: Update =>
      sendToDB(u)

    case d: Delete =>
      sendToDB(d)
  }

  val timeoutBehaviour: Receive = {
    case ReceiveTimeout => complete(StatusCodes.GatewayTimeout, "Server time out")
  }

  def receive: Receive = {
    case FetchReply(succ, content) =>
      succ match {
        case true => complete(StatusCodes.OK, content.get)
        case false => complete(StatusCodes.NotFound, Error("get error"))
      }

    case InsertReply(succ, id) =>
      succ match {
        case true => complete(StatusCodes.Created, HttpIdReply(id.get))
        case false => complete(StatusCodes.BadRequest, Error("Post error"))
      }

    case UpdateReply(succ) =>
      succ match {
        case true => complete(StatusCodes.OK, HttpSuccessReply(succ))
        case false => complete(StatusCodes.NotFound, Error("Put error"))
      }

    case DeleteReply(succ) =>
      succ match {
        case true => complete(StatusCodes.OK, HttpSuccessReply(succ))
        case false => complete(StatusCodes.NotFound, Error("Delete error"))
      }

    case ReceiveTimeout => complete(StatusCodes.GatewayTimeout, Error("Server time out"))
  }

  def sendToDB(msg: Message) = {
    context.setReceiveTimeout(2.second)
    db ! msg
  }

  def complete[T <: AnyRef](status: StatusCode, obj: T) = {
    ctx.complete(status, obj)
    context.stop(self)
  }

}

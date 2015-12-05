package edu.ufl.dos15.fbapi

import akka.actor.{Actor, ActorSelection}
import akka.actor.ReceiveTimeout
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy.Stop
import spray.routing.RequestContext
import spray.http.{StatusCode, StatusCodes}
import org.json4s.Formats
import scala.concurrent.duration._

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

  val timeoutBehaviour: Receive = {
    case ReceiveTimeout => complete(StatusCodes.GatewayTimeout, "Server time out")
  }

  def receive = timeoutBehaviour

  def sendToDB(msg: Message) = {
    context.setReceiveTimeout(2.second)
    db ! msg
  }

  def complete[T <: AnyRef](status: StatusCode, obj: T) = {
    ctx.complete(status, obj)
    context.stop(self)
  }

}

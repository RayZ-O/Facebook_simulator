package edu.ufl.dos15.fbapi.actor

import akka.actor.{Actor, Props, ActorSelection, ReceiveTimeout, OneForOneStrategy}
import akka.actor.SupervisorStrategy.Stop
import spray.routing.{RequestContext, HttpService}
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

  val timeoutBehaviour: Receive = {
    case ReceiveTimeout => complete(StatusCodes.GatewayTimeout, "Server time out")
  }

  def receive: Receive = timeoutBehaviour

  def sendToDB(msg: Message) = {
    context.setReceiveTimeout(2.second)
    db ! msg
  }

  def complete[T <: AnyRef](status: StatusCode, obj: T) = {
    ctx.complete(status, obj)
    context.stop(self)
  }
}

trait RequestActorFactory {
  this: HttpService =>  
  import scala.reflect.ClassTag
  def handle[T](ctx: RequestContext, msg: Message)(implicit ct: ClassTag[T]) = {
    actorRefFactory.actorOf(Props(ct.runtimeClass, ctx, msg))
  }
}

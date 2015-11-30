package edu.ufl.dos15.fbapi

import scala.concurrent.duration._
import akka.actor.{Actor, ActorLogging, ActorSelection, Props, ReceiveTimeout}
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy.Stop
import spray.routing.HttpService
import spray.routing.RequestContext
import spray.http.StatusCode
import spray.http.StatusCodes
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization

class PerRequestActor(ctx: RequestContext, message: Message)
    extends Actor with ActorLogging with Json4sProtocol {
  val db = context.actorSelection("/user/db")
  var objId: String = _
  var putObj: AnyRef = _
  var params: Option[String] = None

  val timeoutBehaviour: Receive = {
    case ReceiveTimeout => complete(StatusCodes.GatewayTimeout, "Server time out")
  }

  def receive = timeoutBehaviour

  message match {
    case Get(id, p) =>
      context.become(timeoutBehaviour orElse waitingFetch)
      objId = id
      params = p
      sendToDB(Fetch(id))

    case Post(obj) =>
      context.become(timeoutBehaviour orElse waitingInsert)
      sendToDB(Insert(Serialization.write(obj)))

    case Put(id, obj) =>
      context.become(timeoutBehaviour orElse waitingUpdatePhase1)
      objId = id
      putObj = obj
      db ! Fetch(id)

    case d: Delete =>
      context.become(timeoutBehaviour orElse waitingDelete)
      sendToDB(d)

    case msg =>
      throw new UnsupportedOperationException(s"Unsupported Operation $msg in per-request actor")

  }

  def sendToDB(msg: Message) = {
    context.setReceiveTimeout(Duration(2, SECONDS))
    db ! msg
  }

  override val supervisorStrategy =
    OneForOneStrategy() {
      case e => {
        complete(StatusCodes.InternalServerError, e.getMessage)
        Stop
      }
  }

  def waitingFetch: Receive = {
    case DBReply(succ, content) =>
      succ match {
        case true =>
          import org.json4s.JsonDSL._
          val json = (JObject() ~ ("id" -> objId)) merge parse(content.get)
          val result = params match {
            case Some(fields) =>
              val query = fields.split(",")
              query.foldLeft(JObject()) { (res, name) =>
                (res ~ (name -> json \ name)) }
            case None => json
          }
          complete(StatusCodes.OK, result)
        case false => complete(StatusCodes.NotFound, Error("get error"))
      }
  }

  def waitingInsert: Receive = {
    case DBReply(succ, id) =>
      succ match {
        case true => complete(StatusCodes.Created, HttpIdReply(id.get))
        case false => complete(StatusCodes.BadRequest, Error("post error"))
      }
  }

  def waitingUpdatePhase1: Receive = {
    case DBReply(succ, content) =>
      succ match {
        case true =>
          context.become(timeoutBehaviour orElse waitingUpdatePhase2)
          val value = Extraction.decompose(putObj)
          val json = parse(content.get)
          val updated = compact(render(json merge value))
          db ! Update(objId, updated)

        case false => complete(StatusCodes.NotFound, Error("put-p1 error"))
      }
  }

  def waitingUpdatePhase2: Receive = {
    case DBReply(succ, content) =>
      succ match {
        case true => complete(StatusCodes.OK, HttpSuccessReply(succ))
        case false => complete(StatusCodes.NotFound, Error("put-p2 error"))
      }
  }

  def waitingDelete: Receive = {
    case DBReply(succ, content) =>
      succ match {
        case true => complete(StatusCodes.OK, HttpSuccessReply(succ))
        case false => complete(StatusCodes.NotFound, Error("delete error"))
      }
  }

  def complete[T <: AnyRef](status: StatusCode, obj: T) = {
    ctx.complete(status, obj)
    context.stop(self)
  }
}

trait PerRequestFactory {
  this: HttpService =>
  def handleRequest(ctx: RequestContext, msg: Message) = {
    actorRefFactory.actorOf(Props(classOf[PerRequestActor], ctx, msg))
  }
}


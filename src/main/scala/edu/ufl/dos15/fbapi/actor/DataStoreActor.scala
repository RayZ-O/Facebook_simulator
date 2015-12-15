package edu.ufl.dos15.fbapi.actor

import scala.concurrent.duration._
import akka.actor.{Actor, ActorLogging, Props}
import spray.routing.HttpService
import spray.routing.RequestContext
import spray.http.StatusCodes
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization
import edu.ufl.dos15.fbapi.Json4sProtocol
import edu.ufl.dos15.fbapi.FBMessage._

class DataStoreActor(reqctx: RequestContext, message: Message) extends Actor
    with ActorLogging with Json4sProtocol with RequestHandler {
  val db = context.actorSelection("/user/db")
  val ctx = reqctx

  var objId: String = _
  var putObj: AnyRef = _
  var idList: List[String] = List.empty
  var params: Option[String] = None

  message match {
    case Get(id, p) =>
      context.become(timeoutBehaviour orElse waitingFetch)
      sendToDB(Fetch(id))

    case Post(obj) =>
      context.become(timeoutBehaviour orElse waitingInsert)
      // TODO

    case Put(id, obj) =>
      //context.become(timeoutBehaviour orElse waitingUpdateFetch)
      // TODO

    case d: Delete =>
      context.become(timeoutBehaviour orElse waitingDelete)
      sendToDB(d)

    case msg =>
      throw new UnsupportedOperationException(s"Unsupported Operation $msg in per-request actor")

  }

  def waitingFetch: Receive = {
    case DBBytesReply(succ, content) => 
      succ match {
        case true => complete(StatusCodes.OK, content.get)
        case false => complete(StatusCodes.NotFound, Error("get error"))
      }
  }

  def waitingInsert: Receive = {
    case DBStrReply(succ, id) =>
      succ match {
        case true => complete(StatusCodes.Created, HttpIdReply(id.get))
        case false => complete(StatusCodes.BadRequest, Error("post error"))
      }
  }

  def waitingUpdate: Receive = {
    case DBBytesReply(succ, content) =>
      succ match {
        case true => complete(StatusCodes.OK, HttpSuccessReply(succ))
        case false => complete(StatusCodes.NotFound, Error("put-p2 error"))
      }
  }

  def waitingDelete: Receive = {
    case DBBytesReply(succ, content) =>
      succ match {
        case true => complete(StatusCodes.OK, HttpSuccessReply(succ))
        case false => complete(StatusCodes.NotFound, Error("delete error"))
      }
  }
}


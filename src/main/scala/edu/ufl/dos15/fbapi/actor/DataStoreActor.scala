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
import edu.ufl.dos15.crypto._

case class AESCred(iv: Array[Byte], ekey: Array[Byte])

class DataStoreActor(reqctx: RequestContext, message: Message, key: Array[Byte]) extends Actor
    with ActorLogging with Json4sProtocol with RequestHandler {
  val db = context.actorSelection("/user/db")
  val ctx = reqctx
  var oid = ""
  var cred: AESCred = _

  message match {
    case gk: GetKey =>
      context.become(timeoutBehaviour orElse waitingFetchCred)
      val pubSubDB = context.actorSelection("pub-sub-db")
      pubSubDB ! gk

    case PostData(id, ed, pt) =>
      context.become(timeoutBehaviour orElse waitingInsert)
      sendToDB(InsertBytes(ed.data))

    case u: Update =>
      context.become(timeoutBehaviour orElse waitingUpdate)
      sendToDB(u)

    case d: Delete =>
      context.become(timeoutBehaviour orElse waitingDelete)
      sendToDB(d)

    case p: PullFeed =>
      context.become(timeoutBehaviour orElse waitingPull)
      sendToDB(p)

    case gp: GetSelfPost =>
      context.become(timeoutBehaviour orElse waitingPull)
      sendToDB(gp)

    case msg =>
      throw new UnsupportedOperationException(s"Unsupported Operation $msg in per-request actor")

  }

  def waitingFetchCred: Receive = {
    case DBCredReply(succ, iv, ekey) =>
      succ match {
        case true =>
          cred = AESCred(iv.get, ekey.get)
          val gk = message.asInstanceOf[GetKey]
          context.become(timeoutBehaviour orElse waitingFetchData)
          sendToDB(Fetch(gk.objId))
        case false => complete(StatusCodes.NotFound, Error("Encryption key not found"))
      }
  }

  def waitingFetchData: Receive = {
    case DBBytesReply(succ, content, _) =>
      succ match {
        case true =>
          complete(StatusCodes.OK, HttpDataReply(content.get, Some(cred.iv), Some(cred.ekey)))
        case false => complete(StatusCodes.NotFound, Error("get error"))
      }
  }

  def waitingInsert: Receive = {
    case DBStrReply(succ, objId, _) =>
      succ match {
        case true =>
          oid = objId.get
          context.become(timeoutBehaviour orElse waitingPublish)
          val pd = message.asInstanceOf[PostData]
          val pubSubDB = context.actorSelection("pub-sub-db")
          pubSubDB ! Publish(pd.id, oid, pd.ed.iv, pd.ed.keys, pd.pType)
        case false => complete(StatusCodes.BadRequest, Error("post error"))
      }
  }

  def waitingPublish: Receive = {
    case DBSuccessReply(succ) =>
      succ match {
        case true => complete(StatusCodes.Created, HttpIdReply(oid))
        case false => complete(StatusCodes.BadRequest, Error("publish error"))
      }
  }

  def waitingUpdate: Receive = {
    case DBSuccessReply(succ) =>
      succ match {
        case true => complete(StatusCodes.OK, HttpSuccessReply(succ))
        case false => complete(StatusCodes.NotFound, Error("update error"))
      }
  }

  def waitingDelete: Receive = {
    case DBSuccessReply(succ) =>
      succ match {
        case true => complete(StatusCodes.OK, HttpSuccessReply(succ))
        case false => complete(StatusCodes.NotFound, Error("delete error"))
      }
  }

  def waitingPull: Receive = {
     case DBListReply(succ, lst) =>
      succ match {
        case true => complete(StatusCodes.OK, HttpListReply(lst.get))
        case false => complete(StatusCodes.NotFound, Error("pull error"))
      }
  }
}


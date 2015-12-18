package edu.ufl.dos15.fbapi.actor

import akka.actor.{Actor, ActorLogging, Props}
import spray.http.StatusCodes
import spray.routing.{RequestContext, HttpService}
import edu.ufl.dos15.fbapi.Json4sProtocol
import edu.ufl.dos15.fbapi.FBMessage._

class FriendListActor(reqctx: RequestContext, message: Message) extends Actor
    with ActorLogging with Json4sProtocol with RequestHandler {
  val db = context.actorSelection("/user/friend-db")
  val ctx = reqctx
  var oid = ""

  message match {
    case f: FetchList =>
       context.become(timeoutBehaviour orElse waitingFetch)
       sendToDB(f)

    case g: GetFriendList =>
      context.become(timeoutBehaviour orElse waitingFetchId)
      sendToDB(g)

    case PostData(id, ed, pt) =>
      val dataDB = context.actorSelection("/user/data-db")
      context.become(timeoutBehaviour orElse waitingInsertData)
      dataDB ! InsertBytes(ed.data)

    case find: FindCommon =>
      context.become(timeoutBehaviour orElse waitingFetch)
      sendToDB(find)

    case PutList(id, ids) =>
      val idList = ids.split(",").toList
      context.become(timeoutBehaviour orElse waitingUpdate)
      sendToDB(UpdateMul(id, idList))

    case d: Delete =>
      context.become(timeoutBehaviour orElse waitingDeleteMeta)
      sendToDB(d)

    case DeleteList(id, ids) =>
      val idList = ids.split(",").toList
      context.become(timeoutBehaviour orElse waitingDelete)
      sendToDB(DeleteMul(id, idList))

    case msg =>
      throw new UnsupportedOperationException(s"Unsupported Operation $msg in friend list actor")
  }

  def waitingFetch: Receive = {
    case DBListReply(succ, list) =>
      succ match {
        case true => complete(StatusCodes.OK, HttpListReply(list.get))
        case false => complete(StatusCodes.NotFound, Error("get error"))
      }
  }

  def waitingFetchId: Receive = {
    case DBStrReply(succ, id, _) =>
      succ match {
        case true => complete(StatusCodes.OK, HttpIdReply(id.get))
        case false => complete(StatusCodes.NotFound, Error("get error"))
      }
  }

  def waitingInsertData: Receive = {
    case DBStrReply(succ, objId, _) =>
      succ match {
        case true =>
          oid = objId.get
          context.become(timeoutBehaviour orElse waitingInsertList)
          val pd = message.asInstanceOf[PostData]
          db ! InsertList(pd.id, objId.get)
        case false => complete(StatusCodes.BadRequest, Error("post error"))
      }
  }

  def waitingInsertList: Receive = {
    case DBStrReply(succ, objId, _) =>
      succ match {
        case true =>
          context.become(timeoutBehaviour orElse waitingPublish)
          val pd = message.asInstanceOf[PostData]
          val pubSubDB = context.actorSelection("/user/pub-sub-db")
          pubSubDB ! Publish(pd.id, objId.get, pd.ed.iv, pd.ed.keys, pd.pType)
        case false => complete(StatusCodes.BadRequest, Error("post error"))
      }
  }

  def waitingPublish: Receive = {
    case DBSuccessReply(succ) =>
      succ match {
        case true =>
          complete(StatusCodes.Created, HttpIdReply(oid))
        case false => complete(StatusCodes.BadRequest, Error("post error"))
      }
  }

  def waitingUpdate: Receive = {
     case DBSuccessReply(succ) =>
      succ match {
        case true => complete(StatusCodes.OK, HttpSuccessReply(succ))
        case false => complete(StatusCodes.NotFound, Error("update error"))
      }
  }

  def waitingDeleteMeta: Receive = {
     case DBSuccessReply(succ) =>
      succ match {
        case true =>
          context.become(timeoutBehaviour orElse waitingDelete)
           val dataDB = context.actorSelection("/user/data-db")
           val oid = message.asInstanceOf[Delete].objId
           dataDB ! Delete(oid)
        case false => complete(StatusCodes.NotFound, Error("delete meta error"))
      }
  }

   def waitingDelete: Receive = {
    case DBSuccessReply(succ) =>
      succ match {
        case true => complete(StatusCodes.OK, HttpSuccessReply(succ))
        case false => complete(StatusCodes.NotFound, Error("delete error"))
      }
  }
}


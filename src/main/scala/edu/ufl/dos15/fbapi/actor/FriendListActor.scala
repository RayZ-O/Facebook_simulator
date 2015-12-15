package edu.ufl.dos15.fbapi.actor

import akka.actor.{Actor, ActorLogging, Props}
import spray.routing.{RequestContext, HttpService}
import edu.ufl.dos15.fbapi.Json4sProtocol
import edu.ufl.dos15.fbapi.FBMessage._
import scala.reflect.ClassTag

class FriendListActor(reqctx: RequestContext, message: Message) extends Actor
    with ActorLogging with Json4sProtocol with RequestHandler {
  val db = context.actorSelection("/user/frienddb")
  val ctx = reqctx

  val matcher: PartialFunction[Any, Unit] = {
    case find: FindCommon =>
      sendToDB(find)

    case PutList(id, ids) =>
      val idList = ids.split(",").toList
      sendToDB(UpdateMul(id, idList))

    case DeleteList(id, ids) =>
      val idList = ids.split(",").toList
      sendToDB(DeleteMul(id, idList))

    case msg =>
      throw new UnsupportedOperationException(s"Unsupported Operation $msg in friend list actor")
  }

  matcher(message)

}


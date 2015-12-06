package edu.ufl.dos15.fbapi.actor

import akka.actor.{Actor, ActorLogging, Props}
import spray.routing.RequestContext
import edu.ufl.dos15.fbapi._

class FriendListActor(reqctx: RequestContext, message: Message) extends Actor
    with ActorLogging with Json4sProtocol with RequestHandler {
  val db = context.actorSelection("/user/frienddb")
  val ctx = reqctx

  val extendMatcher: PartialFunction[Any, Unit] = {
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

  defaultMatcher.orElse(extendMatcher)(message)

}

package edu.ufl.dos15.db

import akka.actor.{Actor, ActorLogging}
import edu.ufl.dos15.fbapi.FBMessage._

class FriendDB extends Actor with ActorLogging {

  class FriendStore(listInfo: String) {
    var info = listInfo
    import scala.collection.mutable.HashSet
    var data = new HashSet[String]
  }

  import scala.collection.mutable.HashMap
  private var friendDB = new HashMap[String, FriendStore]
  private var count = 0

  def receive = {
    case Fetch(id) => // TODO

    case FindCommon(id1, id2) =>
      if (friendDB.contains(id1) && friendDB.contains(id2)) {
        val common = friendDB(id1).data & friendDB(id2).data
      } else {
        sender ! FetchReply(false)
      }

    case Insert(value) =>
        sender ! InsertReply(true, Some(insert(value)))

    case Update(id, info) =>
      friendDB.get(id) match {
        case Some(fl) =>
          fl.info = info
          sender ! UpdateReply(true)
        case None => sender ! UpdateReply(false)
      }

    case UpdateMul(id, idList) =>
      friendDB.get(id) match {
        case Some(fl) =>
          idList foreach { id => fl.data += id }
          sender ! UpdateReply(true)
        case None => sender ! UpdateReply(false)
      }

    case DeleteMul(id, idList) =>
      friendDB.get(id) match {
        case Some(fl) =>
          idList foreach { id => fl.data -= id }
          sender ! DeleteReply(true)
        case None => sender ! DeleteReply(false)
      }
  }

  def insert(value: String) = {
      count += 1
      val id = System.currentTimeMillis().toString + count
      friendDB += (id -> new FriendStore(value))
      id
  }
}

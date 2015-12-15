package edu.ufl.dos15.db

import akka.actor.{Actor, ActorLogging}
import edu.ufl.dos15.fbapi.FBMessage._

class FriendDB extends Actor with ActorLogging {

  class FriendList(listInfo: String) {
    var info = listInfo
    import scala.collection.mutable.HashSet
    var data = new HashSet[String]
  }

  import scala.collection.mutable.HashMap
  private var friendDB = new HashMap[String, FriendList]
  import scala.collection.mutable.ListBuffer
  private var owenrToList = new HashMap[String, ListBuffer[String]]
  private var count = 0

  def receive = {
    case Fetch(id) => // TODO

    case FindCommon(id1, id2) =>
      if (friendDB.contains(id1) && friendDB.contains(id2)) {
        val common = friendDB(id1).data & friendDB(id2).data
        // TODO sender ! 
      } else {
        sender ! DBStrReply(false)
      }

    case InsertStr(value) =>
        sender ! DBStrReply(true, Some(insert(value)))

    case Update(id, info) =>
      friendDB.get(id) match {
        case Some(fl) =>
          // fl.info = info TODO
          sender ! DBStrReply(true)
        case None => sender ! DBStrReply(false)
      }

    case UpdateMul(id, idList) =>
      friendDB.get(id) match {
        case Some(fl) =>
          idList foreach { id => fl.data += id }
          sender ! DBStrReply(true)
        case None => sender ! DBStrReply(false)
      }

    case DeleteMul(id, idList) =>
      friendDB.get(id) match {
        case Some(fl) =>
          idList foreach { id => fl.data -= id }
          sender ! DBStrReply(true)
        case None => sender ! DBStrReply(false)
      }
  }

  def insert(value: String) = {
      count += 1
      val id = System.currentTimeMillis().toString + count
      friendDB += (id -> new FriendList(value))
      id
  }
}

package edu.ufl.dos15.db

import akka.actor.{Actor, ActorLogging}
import edu.ufl.dos15.fbapi.FBMessage._

class EncryptedDataDB extends Actor with ActorLogging {
  val dbNo = "002"
  import scala.collection.mutable.HashMap
  private var db = new HashMap[String, Array[Byte]]
  private var sequenceNum = 0;

  def receive: Receive = {
    case Fetch(id) =>
      db.get(id) match {
        case Some(value) =>
          sender ! DBBytesReply(true, Some(value))
        case None =>
          sender ! DBBytesReply(false)
      }

    case InsertBytes(value) =>
      sequenceNum += 1
      val id = dbNo + System.currentTimeMillis().toString + sequenceNum
      db += (id -> value)
      sender ! DBStrReply(true, Some(id))

    case Update(id, newValue) =>
      db += (id -> newValue)
      sender ! DBSuccessReply(true)

    case Delete(id) =>
      if (db.contains(id)) {
        db -= id
        sender ! DBSuccessReply(true)
      } else {
        sender ! DBSuccessReply(false)
      }

    case DBTestInsert(id, value) =>
      if (!db.contains(id)) {
        sequenceNum += 1
        db += (id -> value)
        sender ! DBSuccessReply(true)
      }
  }
}

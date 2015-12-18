package edu.ufl.dos15.db

import akka.actor.{Actor, ActorLogging}
import edu.ufl.dos15.fbapi.FBMessage._

class PubSubDB extends Actor with ActorLogging {
  import scala.collection.mutable.HashMap
  var ivDB = new HashMap[String, Array[Byte]]
  import scala.collection.mutable.LinkedHashMap
  var feedChans = new HashMap[String, LinkedHashMap[String, Array[Byte]]]
  var profileChans = new HashMap[String, HashMap[String, Array[Byte]]]
  var friendChans = new HashMap[String, HashMap[String, Array[Byte]]]
  var selfPostChans = new HashMap[String, HashMap[String, Array[Byte]]]

  def receive = {
    case Publish(ownerId, objId, iv, keys, pType) =>
      ivDB += objId -> iv
      pType match {
        case "feed" =>
          val ownerKey = keys(ownerId)
          addToChan(selfPostChans, objId, ownerId, ownerKey)
          keys foreach { case (id, key) => addToChan(feedChans, objId, id, key) }
        case "profile" => keys foreach { case (id, key) => addToChan(profileChans, objId, id, key) }
        case "friend" => keys foreach { case (id, key) => addToChan(friendChans, objId, id, key) }
      }
      sender ! DBSuccessReply(true)

    case CreateChannel(pId, iv, key) =>
      ivDB += pId -> iv
      feedChans += (pId -> LinkedHashMap.empty)
      profileChans += (pId -> HashMap(pId->key))
      friendChans += (pId -> HashMap.empty)
      selfPostChans += (pId -> HashMap.empty)
      println(profileChans)
      sender ! DBSuccessReply(true)

    case GetKey(ownerId, objId, pType) =>
      val iv = ivDB.get(objId)
      val ekey = pType match {
        case "feed" => if (feedChans.contains(ownerId)) feedChans(ownerId).get(objId) else None
        case "profile" => if (profileChans.contains(ownerId)) profileChans(ownerId).get(objId) else None
        case "friend" => if (friendChans.contains(ownerId)) friendChans(ownerId).get(objId) else None
      }
      if (iv.isDefined && ekey.isDefined) {
        sender ! DBCredReply(true, iv, ekey)
      } else {
        sender ! DBCredReply(false)
      }

    case PullFeed(id, start) =>
      feedChans.get(id) match {
        case Some(m) =>
          val ids = m.slice(m.size - start - 5, m.size - start).keySet.toList
          sender ! DBListReply(true, Some(ids))
        case None => sender ! DBListReply(false)
      }

    case GetSelfPost(id) =>
      selfPostChans.get(id) match {
        case Some(p) =>
           val ids = p.keySet.toList
           sender ! DBListReply(true, Some(ids))
        case None => sender ! DBListReply(false)
      }
  }

  def addToChan[T <: collection.mutable.Map[String, Array[Byte]]](chan: HashMap[String, T],
      objId: String, userId: String, key: Array[Byte]) = {
    chan.get(userId) match {
      case Some(m) => m += objId -> key
      case None => //nothing to do
    }
  }
}

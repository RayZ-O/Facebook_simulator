package edu.ufl.dos15.db

import akka.actor.{Actor, ActorLogging}
import edu.ufl.dos15.fbapi.FBMessage._

class PubSubDB extends Actor with ActorLogging {
  import scala.collection.mutable.HashMap
  import scala.collection.mutable.ListMap
  var ivDB = new HashMap[String, Array[Byte]]
  var feedChans = new HashMap[String, ListMap[String, Array[Byte]]]
  var profileChans = new HashMap[String, HashMap[String, Array[Byte]]]
  var selfChans = new HashMap[String, HashMap[String, Array[Byte]]]
  
  def receive = {
    case Publish(ownerId, ownerKey, objId, iv, keys, pType) => 
      ivDB += objId -> iv
      addToChan(selfChans, objId, ownerId, ownerKey)      
      pType match {
        case "feed" => keys foreach { case (id, key) => addToChan(feedChans, objId, id, key) }
        case "profile" => keys foreach { case (id, key) => addToChan(profileChans, objId, id, key) }
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

package edu.ufl.dos15.db

import akka.actor.{Actor, ActorLogging}
import edu.ufl.dos15.fbapi._

case class Feed(id: String, key: String)

class PubSubDB extends Actor with ActorLogging {
  
  import scala.collection.mutable.HashMap
  val keys = new HashMap[String, HashMap[String, String]]
  val channels = new HashMap[String, Feed]
  
  def receive = {
    ???
  }
}
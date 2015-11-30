package edu.ufl.dos15.client

import akka.actor.{Actor, ActorLogging, Props}

case object Start

class Simulator(host: String, port: Int) extends Actor with ActorLogging {

  def receive: Receive = {
    case Start =>
      log.info("Simulator start")
      val id = "TODO"
      val client = context.actorOf(Props[Client], id)
      client ! Start

    case _ =>
  }
}

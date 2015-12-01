package edu.ufl.dos15.client

import akka.actor.{Actor, ActorLogging, Props}

case object Start
case class Register(id: String)

class Simulator(host: String, port: Int) extends Actor with ActorLogging {

  def receive: Receive = {
    case Start =>
      log.info("Simulator start")


    case Register(id) =>
      val client = context.actorOf(Props(classOf[Client], id, host, port, true))
      client ! Start

    case _ =>
  }
}

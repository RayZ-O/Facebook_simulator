package edu.ufl.dos15.client

import scala.util.Try
import akka.actor.{ActorSystem, Props, Inbox}
import com.typesafe.config.ConfigFactory

object Main {
    def main(args: Array[String]): Unit = {
        val config = ConfigFactory.load()
        val serverHost = Try(config.getString("server.host")).getOrElse("localhost")
        val serverPort = Try(config.getInt("server.port")).getOrElse(8080)

        implicit val system = ActorSystem("FacebookClientSystem")
        val simulator = system.actorOf(Props(classOf[Simulator], serverHost, serverPort), "simulator")
        val inbox = Inbox.create(system)
        inbox.send(simulator, Start)

    }
}

package edu.ufl.dos15.client

import scala.util.Try
import akka.actor.{ActorSystem, Props, Inbox}
import com.typesafe.config.ConfigFactory


import edu.ufl.dos15.fbapi.Json4sProtocol
object Main extends Json4sProtocol {
    def main(args: Array[String]): Unit = {
        val config = ConfigFactory.load()
        val serverHost = Try(config.getString("server.host")).getOrElse("localhost")
        val serverPort = Try(config.getInt("server.port")).getOrElse(8080)
        if (args.length < 1) {
            println("usage: sbt \"run [num of users]\"")
        }
        val numOfUsers = args(0).toInt
        implicit val system = ActorSystem("FacebookClientSystem")
        val simulator = system.actorOf(Props(classOf[Simulator], serverHost, serverPort, numOfUsers), "simulator")
        val inbox = Inbox.create(system)
        inbox.send(simulator, Start)
    }
}

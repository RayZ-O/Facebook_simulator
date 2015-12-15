package edu.ufl.dos15.fbapi

import scala.util.Try
import akka.actor.{ActorSystem, Props}
import akka.io.IO
import spray.can.Http
import com.typesafe.config.ConfigFactory
import  edu.ufl.dos15.db._

object Main {
    def main(args: Array[String]): Unit = {
        val config = ConfigFactory.load()
        val serverHost = Try(config.getString("server.host")).getOrElse("localhost")
        val serverPort = Try(config.getInt("server.port")).getOrElse(8080)
        implicit val system = ActorSystem("FacebookSystem")
        // initialize database
        val authDB = system.actorOf(Props[AuthDB], "auth-db")
        val dataDB = system.actorOf(Props[EncryptedDataDB], "data-db")
        val friendDB = system.actorOf(Props[FriendDB], "friend-db")
        val pubSubDB = system.actorOf(Props[PubSubDB], "pub-sub-db")
        // initialize server
        val server = system.actorOf(Props[Server], "server")
        IO(Http) ! Http.Bind(server, serverHost, serverPort)
    }
}

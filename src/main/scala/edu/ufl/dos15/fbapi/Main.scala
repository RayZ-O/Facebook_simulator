package edu.ufl.dos15.fbapi

import scala.util.Try
import akka.actor.{ActorSystem, Props}
import akka.io.IO
import spray.can.Http
import com.typesafe.config.ConfigFactory

object Main {
    def main(args: Array[String]): Unit = {
        val config = ConfigFactory.load()
        val serverHost = Try(config.getString("server.host")).getOrElse("localhost")
        val serverPort = Try(config.getInt("server.port")).getOrElse(8080)

        implicit val system = ActorSystem("FacebookSystem")
        val db = system.actorOf(Props[MockDB], "db")
        val server = system.actorOf(Props[Server], "server")

        import edu.ufl.dos15.fbapi.FriendListService._
        import edu.ufl.dos15.fbapi.FriendListService.FriendListType.CLOSE_FRIENDS
        import org.json4s.native.Serialization
        import org.json4s._
        import org.json4s.ext.EnumSerializer
        implicit def json4sFormats: Formats = DefaultFormats +
                                          new EnumSerializer(FriendListService.FriendListType)
        db ! DBTestInsert("1", Serialization.write(FriendList(name=Some("ss"),list_type=Some(CLOSE_FRIENDS))))
        IO(Http) ! Http.Bind(server, serverHost, serverPort)
    }
}

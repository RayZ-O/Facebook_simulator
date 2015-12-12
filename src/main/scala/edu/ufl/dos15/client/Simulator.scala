package edu.ufl.dos15.client

import akka.actor.{Actor, ActorLogging, Props}
import spray.client.pipelining._
import scala.util.{Success, Failure}
import scala.util.Random
import edu.ufl.dos15.fbapi.Json4sProtocol


case object Start
case object UserCreated
case class Register(id: String)
case class Run(ids: List[String])

class Simulator(host: String, port: Int, num: Int) extends Actor with ActorLogging with Json4sProtocol {
  import context.dispatcher

  import edu.ufl.dos15.fbapi.UserService.User
  import edu.ufl.dos15.fbapi.FBMessage.HttpIdReply
  val pipeline = sendReceive ~> unmarshal[HttpIdReply]
  val userUri = s"http://$host:$port/user"

  import scala.collection.mutable.ListBuffer
  var userIds = new ListBuffer[String]
  var count = 0

  def receive: Receive = {
    case Start =>
      for(i <- 1 to num) {
        createUser()
      }

    case UserCreated =>
      count += 1
      if (count == num) {
        count = 0
        userIds.foreach{ id =>
          val client = context.actorOf(Props(classOf[Client], id, host, port, false))
          client ! Run(Random.shuffle(userIds).take(50).toList)
        }
      }

    case Register(id) =>
      val client = context.actorOf(Props(classOf[Client], id, host, port, true))
      client ! Start

    case _ =>
  }

  def createUser() = {
    val user = User(email=Some(randomString(8)),
                   first_name=Some(randomString(5)),
                   last_name=Some(randomString(5)),
                   gender=Random.nextInt(2) match {
                      case 0 => Some("male")
                      case 1 => Some("female")},
                   verified=Some(false),
                   locale=Some("en/US"))

    val responseFuture = pipeline { Post(userUri, user) }
    responseFuture onComplete {
      case Success(rep) =>
        userIds += rep.id
        self ! UserCreated
      case Failure(error) =>
        log.error(error, "Couldn't create user")
    }
  }

  def randomString(length: Int) = {
    Random.alphanumeric.take(length).mkString
  }
}

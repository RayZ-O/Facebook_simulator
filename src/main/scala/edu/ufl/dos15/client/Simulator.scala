package edu.ufl.dos15.client

import java.security.KeyPair
import akka.actor.{Actor, ActorLogging, Props, ActorRef}
import spray.client.pipelining._
import scala.util.{Success, Failure}
import scala.util.Random
import org.json4s.native.Serialization
import edu.ufl.dos15.fbapi.Json4sProtocol
import edu.ufl.dos15.crypto.Crypto._
import edu.ufl.dos15.fbapi.FBMessage._
import java.security.PublicKey

case object Start
case object UserRegistered
case class Run(ids: List[Tuple2[String, PublicKey]])

class Simulator(host: String, port: Int, num: Int) extends Actor with ActorLogging with Json4sProtocol {
  import context.dispatcher

  import edu.ufl.dos15.fbapi.UserService.User
  import edu.ufl.dos15.fbapi.FBMessage.HttpIdReply
  val pipeline = sendReceive ~> unmarshal[HttpIdReply]
  val userUri = s"http://$host:$port/user"

  import scala.collection.mutable.ListBuffer
  var userIds = new ListBuffer[Tuple2[String, KeyPair]]
  var clients = new ListBuffer[ActorRef]
  var count = 0

  def receive: Receive = {
    case Start =>
      for(i <- 1 to num) {
        register()
      }

    case UserRegistered =>
      count += 1
      if (count == num) {
        count = 0
        userIds.foreach{ p =>
          val client = context.actorOf(Props(classOf[Client], p._1, host, port, p._2))
          clients += client
          client ! Run(Random.shuffle(userIds).take(50).collect{case p => (p._1, p._2.getPublic())}.toList)
        }
      }

    case _ =>
  }

  def register() = {
    val user = User(email=Some(randomString(8)),
                    first_name=Some(randomString(5)),
                    last_name=Some(randomString(5)),
                    gender=Random.nextInt(2) match {
                      case 0 => Some("male")
                      case 1 => Some("female")},
                    verified=Some(false),
                    locale=Some("en/US"))
    val kp = RSA.generateKeyPair()
    val data = Serialization.write(user)
    val iv = AES.generateIv()
    val symKey = AES.generateKey()
    val encrypted = signedEncryptAES(data, kp.getPrivate(), symKey, iv, kp.getPublic())
    val ekey = RSA.encrypt(symKey.getEncoded(), kp.getPublic())
    val reg = RegisterUser(encrypted, iv.getIV(), ekey, kp.getPublic().getEncoded())
    val pipeline = sendReceive ~> unmarshal[HttpIdReply]
    val responseFuture = pipeline( Post(s"http://$host:$port/register", reg) )
    responseFuture onComplete {
      case Success(rep) =>
        userIds += Tuple2(rep.id, kp)
        self ! UserRegistered
      case Failure(error) =>
        log.error(error, "Couldn't register user")
    }
  }

  def randomString(length: Int) = {
    Random.alphanumeric.take(length).mkString
  }
}

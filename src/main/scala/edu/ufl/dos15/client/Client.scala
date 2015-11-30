package edu.ufl.dos15.client

import akka.actor.{Actor, ActorLogging}
import spray.http.StatusCodes
import spray.client.pipelining._
import scala.util.{Success, Failure}
import scala.util.Random
import edu.ufl.dos15.fbapi.Json4sProtocol

class Client(id: String) extends Actor with ActorLogging with Json4sProtocol {
  import context.dispatcher
  import edu.ufl.dos15.fbapi.UserService._
  import edu.ufl.dos15.fbapi.PageService._
  import edu.ufl.dos15.fbapi.FeedService._
  import edu.ufl.dos15.fbapi.FriendListService._

  val userUri = "http://localhost:8080/user"
  val pageUri = "http://localhost:8080/page"
  val feedUri = "http://localhost:8080/feed"
  val friendUri = "http://localhost:8080/friends"

  import scala.collection.mutable.ArrayBuffer
  var myPost = new ArrayBuffer[String]
  var myFriend = new ArrayBuffer[String]

  val pipeline = sendReceive

  def receive: Receive = {
    case Start => getUser()
    case _ =>
  }

  def getUser() {
    val responseFuture = pipeline { Get(userUri) }
    responseFuture onComplete {
      case Success(res) =>
        log.info(s"status: ${res.status}")
      case Failure(error) =>
        log.error(error, "Couldn't get user")
    }
  }

  def updateUser() {
    val user = generateUpdate("user")
    val responseFuture = pipeline { Put(userUri+id, user) }
    responseFuture onComplete {
      case Success(res) =>
        log.info(s"status: ${res.status}")
      case Failure(error) =>
        log.error(error, "Couldn't put user")
    }
  }

  def deleteUser() {
    val responseFuture = pipeline { Delete(userUri+id) }
    responseFuture onComplete {
      case Success(res) =>
        if (res.status == StatusCodes.OK) {
          context.stop(self)
        }
      case Failure(error) =>
        log.error(error, "Couldn't delete user")
    }
  }

  def getPage() {
    val responseFuture = pipeline { Get(pageUri) }
    responseFuture onComplete {
      case Success(res) =>
        log.info(s"status: ${res.status}")
      case Failure(error) =>
        log.error(error, "Couldn't get page")
    }
  }

  def createPage() {
    val page = generateUpdate("page")
    val responseFuture = pipeline { Post(pageUri, page) }
    responseFuture onComplete {
      case Success(res) =>
        log.info(s"status: ${res.status}")
      case Failure(error) =>
        log.error(error, "Couldn't post page")
    }
  }

  def updatePage() {
    val page = generateUpdate("page")
    val responseFuture = pipeline { Put(pageUri+id, page) }
    responseFuture onComplete {
      case Success(res) =>
        log.info(s"status: ${res.status}")
      case Failure(error) =>
        log.error(error, "Couldn't put page")
    }
  }

  def deletePage() {
    val responseFuture = pipeline { Delete(pageUri+id) }
    responseFuture onComplete {
      case Success(res) =>
        log.info(s"status: ${res.status}")
      case Failure(error) =>
        log.error(error, "Couldn't delete page")
    }
  }

  def getFeed() {
    val responseFuture = pipeline { Get(feedUri) }
    responseFuture onComplete {
      case Success(res) =>
        log.info(s"status: ${res.status}")
      case Failure(error) =>
        log.error(error, "Couldn't get feed")
    }
  }

  def createFeed() {
    val feed = generateUpdate("feed")
    val responseFuture = pipeline { Post(feedUri, feed) }
    responseFuture onComplete {
      case Success(res) =>
        log.info(s"status: ${res.status}")
      case Failure(error) =>
        log.error(error, "Couldn't post feed")
    }
  }

  def updateFeed() {
    val feed = generateUpdate("feed")
    val responseFuture = pipeline { Put(feedUri+id, feed) }
    responseFuture onComplete {
      case Success(res) =>
        log.info(s"status: ${res.status}")
      case Failure(error) =>
        log.error(error, "Couldn't put feed")
    }
  }

  def deleteFeed() {
    val feedId = myPost(Random.nextInt(myPost.length))
    val responseFuture = pipeline { Delete(feedUri+feedId) }
    responseFuture onComplete {
      case Success(res) =>
         if (res.status == StatusCodes.OK) {
           myPost -= feedId
         }
      case Failure(error) =>
        log.error(error, "Couldn't delete feed")
    }
  }

  def getFriends() {
    val responseFuture = pipeline { Get(friendUri) }
    responseFuture onComplete {
      case Success(res) =>
        log.info(s"status: ${res.status}")
      case Failure(error) =>
        log.error(error, "Couldn't get friend")
    }
  }

  def createFriends() {
    val responseFuture = pipeline { Post(friendUri) }
    responseFuture onComplete {
      case Success(res) =>
        log.info(s"status: ${res.status}")
      case Failure(error) =>
        log.error(error, "Couldn't post friend")
    }
  }

  def updateFriends() {
    val ids = generateUpdate("friend")
    val responseFuture = pipeline { Put(friendUri+ids) }
    responseFuture onComplete {
      case Success(res) =>
        log.info(s"status: ${res.status}")
      case Failure(error) =>
        log.error(error, "Couldn't put friend")
    }
  }

  def deleteFriends() {
    val friendId = ""//TODO get friend
    val responseFuture = pipeline { Delete(friendUri+friendId) }
    responseFuture onComplete {
      case Success(res) =>
        log.info(s"status: ${res.status}")
      case Failure(error) =>
        log.error(error, "Couldn't delete friend")
    }
  }

  def generateUpdate(t: String) = {
    t match {
      case "user" =>
        val email = Some(Random.nextString(8))
        User(email=email)
      case "page" =>
        val name = Some(Random.nextString(8))
        Page(name=name)
      case "feed" =>
        val message = Some(Random.nextString(50))
        Feed(message=message)
      case "friends" =>
        val name = Some(Random.nextString(8))
        FriendList(name=name)
    }
  }
}


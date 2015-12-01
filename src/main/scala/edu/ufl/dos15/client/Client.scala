package edu.ufl.dos15.client

import akka.actor.{Actor, ActorLogging}
import spray.http.StatusCodes
import spray.http.HttpResponse
import spray.client.pipelining._
import scala.util.{Success, Failure}
import scala.concurrent.Future
import scala.util.Random

import edu.ufl.dos15.fbapi.Json4sProtocol
import edu.ufl.dos15.fbapi.HttpIdReply

class Client(id: String, host: String, port: Int, page: Boolean) extends Actor with ActorLogging with Json4sProtocol {
  import context.dispatcher
  import edu.ufl.dos15.fbapi.UserService._
  import edu.ufl.dos15.fbapi.PageService._
  import edu.ufl.dos15.fbapi.FeedService._
  import edu.ufl.dos15.fbapi.FriendListService._

  val userUri = s"http://$host:$port/user"
  val pageUri = s"http://$host:$port/page"
  val feedUri = s"http://$host:$port/feed"
  val friendUri = s"http://$host:$port/friends"
  val edgeBaseUri = if (page == true) s"http://$host:$port/page/$id"
                else s"http://$host:$port/user/$id"

  import scala.collection.mutable.ArrayBuffer
  var myPost = new ArrayBuffer[String]
  var myFriend = new ArrayBuffer[String]
  var myFriendListId = ""

  val pipeline = sendReceive

  def receive: Receive = {
    case Start =>
    case _ =>
  }

  def getUser(userId: String) {
    val responseFuture = pipeline { Get(userUri + "/" + userId) }
    responseHandler(responseFuture,
                    s"Get user $userId failed",
                    s"Couldn't get user $userId")
  }

  def updateUser() {
    val user = generateUpdate("user")
    val responseFuture = pipeline { Put(userUri + "/" + id, user) }
    responseHandler(responseFuture,
                    s"Update user $id failed",
                    s"Couldn't put user $id ")
  }

  def deleteUser() {
    val responseFuture = pipeline { Delete(userUri + "/" + id) }
    stopHandler(responseFuture,
                s"Delete user $id failed",
                s"Couldn't delete user $id")
  }

  def getPage(pageId: String) {
    val responseFuture = pipeline { Get(pageUri + "/" + pageId) }
    responseHandler(responseFuture,
                    s"Get page $pageId failed",
                    s"Couldn't get page $pageId")
  }

  def createPage() {
    val page = generateUpdate("page")
    val pipeline = sendReceive ~> unmarshal[HttpIdReply]
    val responseFuture = pipeline { Post(pageUri, page)  }
    responseFuture onComplete {
      case Success(rep) =>
        context.actorSelection("../") ! Register(rep.id)
      case Failure(error) =>
        log.error(error, "Couldn't post page")
    }
  }

  def updatePage() {
    val page = generateUpdate("page")
    val responseFuture = pipeline { Put(pageUri + "/" + id, page) }
    responseHandler(responseFuture,
                s"Update page $id failed",
                s"Couldn't put page $id")
  }

  def deletePage() {
    val responseFuture = pipeline { Delete(pageUri + "/" + id) }
    stopHandler(responseFuture,
                s"Delete page $id failed",
                s"Couldn't delete page $id")
  }

  def getFeed() {
    val responseFuture = pipeline { Get(feedUri + "/" + "new") }
    responseFuture onComplete {
      case Success(res) =>
        log.info(s"status: ${res.status}")
      case Failure(error) =>
        log.error(error, "Couldn't get feed")
    }
  }

  def createFeed() {
    val feed = generateUpdate("feed")
    val responseFuture = pipeline { Post(edgeBaseUri + "/" + "feed", feed) }
    responseHandler(responseFuture,
                s"Create feed failed",
                s"Couldn't post feed")
  }

  def updateFeed(feedId: String) {
    val feed = generateUpdate("feed")
    val responseFuture = pipeline { Put(feedUri + "/" + feedId, feed) }
    responseHandler(responseFuture,
                s"Update feed failed",
                s"Couldn't put feed")
  }

  def deleteFeed() {
    val feedId = myPost(Random.nextInt(myPost.length))
    val responseFuture = pipeline { Delete(feedUri + "/" + feedId) }
    responseFuture onComplete {
      case Success(res) =>
         if (res.status == StatusCodes.OK) {
           myPost -= feedId
         }
      case Failure(error) =>
        log.error(error, "Couldn't delete feed")
    }
  }

  def getFriends(friendListId: String) {
    val responseFuture = pipeline { Get(friendUri + "/" + friendListId) }
    responseHandler(responseFuture,
                s"Get friend list $friendListId failed",
                s"Couldn't get friend list $friendListId")
  }

  def createFriends() {
    val friends = generateUpdate("friends")
    val responseFuture = pipeline { Post(edgeBaseUri + "/" + "friends", friends) }
    responseFuture onComplete {
      case Success(res) =>
        log.info(s"status: ${res.status}")
      case Failure(error) =>
        log.error(error, "Couldn't post friend")
    }
  }

  def updateFriends() {
    val ids = myFriend.reduceLeft(_+","+_)
    val responseFuture = pipeline { Put(friendUri + "/" + myFriendListId + "?ids=" + ids) }
     responseHandler(responseFuture,
                    s"Put friends to friend list $myFriendListId failed",
                    s"Couldn't put friends to friend list $myFriendListId")
  }

  def deleteFriends() = {
    val responseFuture = pipeline { Delete( friendUri + "/" + myFriendListId) }
    responseHandler(responseFuture,
                    s"Delete friend list $myFriendListId failed",
                    s"Couldn't delete friend list $myFriendListId")
  }

  def responseHandler(f: Future[HttpResponse], errMsg1: String, errMsg2: String) = {
    f onComplete {
      case Success(res) =>
        if (res.status != StatusCodes.OK) {
          log.error(errMsg1)
        }
      case Failure(error) =>
        log.error(error, errMsg2)
    }
  }

  def stopHandler(f: Future[HttpResponse], errMsg1: String, errMsg2: String) = {
    f onComplete {
      case Success(res) =>
        if (res.status == StatusCodes.OK) {
          context.stop(self)
        } else {
          log.error(errMsg1)
        }
      case Failure(error) =>
        log.error(error, errMsg2)
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


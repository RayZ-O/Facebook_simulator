package edu.ufl.dos15.fbapi

import org.specs2.mutable.{Specification, Before}
import spray.testkit.Specs2RouteTest
import spray.http.StatusCodes._
import scala.concurrent.duration._
import akka.actor.{ActorSystem, Props}

class FriendListServiceSpec extends Specification with Specs2RouteTest with FriendListService with Before {
  import FriendListService._

  implicit val routeTestTimeout = RouteTestTimeout(FiniteDuration(5, SECONDS))

  def actorRefFactory = system

  def before() = {
    val db = system.actorOf(Props[MockDB], "db")
    db ! DBTestInsert("41", """{"name": "myfriends"}""")
  }

  sequential

  "The FriendListService" should {

    "return OK for GET requests to /friends" in {
      Get("/friends") ~> friendListRoute ~> check {
        response.status should be equalTo OK
      }
    }

    "return id for POST requests to /friends" in {
      Post("/friends", FriendList()) ~> friendListRoute ~> check {
        response.status should be equalTo Created
        response.entity should not be equalTo(None)
        val reply = responseAs[HttpIdReply]
        reply.id.equals("") should be equalTo(false)
      }
    }

    "return all fileds for GET request to /friends/{id}" in {
      Get("/friends/41") ~> friendListRoute ~> check {
        response.status should be equalTo OK
        response.entity should not be equalTo(None)
        val friends = responseAs[FriendList]
        friends === FriendList(id=Some("41"),
                      name=Some("myfriends")
                      )
      }
    }

    "return success for PUT request to existed id" in {
      Put("/friends/41", FriendList(name=Some("myfriends"))) ~> friendListRoute ~> check {
        response.status should be equalTo OK
        response.entity should not be equalTo(None)
        val reply = responseAs[HttpSuccessReply]
        reply.success should be equalTo(true)
      }
    }

    "return specific fileds for GET request to /friends/{id}?<fileds>" in {
      Get("/friends/41?fields=name") ~> friendListRoute ~> check {
        response.status should be equalTo OK
        response.entity should not be equalTo(None)
        val friends = responseAs[FriendList]
        friends.name.getOrElse("") === "myfriends"
      }
    }

    "return success for DELETE request to existed id" in {
      Delete("/friends/41") ~> friendListRoute ~> check {
        response.status should be equalTo OK
        response.entity should not be equalTo(None)
        val reply = responseAs[HttpSuccessReply]
        reply.success should be equalTo(true)
      }
    }

    "return NotFound for GET request to non-existed id" in {
      Get("/friends/2") ~> friendListRoute ~> check {
        response.status should be equalTo NotFound
      }
    }

    "return NotFound for PUT request to non-existed id" in {
      Put("/friends/2", FriendList(name=Some("myfriends"))) ~> friendListRoute ~> check {
        response.status should be equalTo NotFound
      }
    }

    "return NotFound for DELETE request to non-existed id" in {
      Delete("/friends/2") ~> friendListRoute ~> check {
        response.status should be equalTo NotFound
      }
    }
  }
}

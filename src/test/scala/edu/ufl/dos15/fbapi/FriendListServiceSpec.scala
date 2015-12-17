package edu.ufl.dos15.fbapi

import org.specs2.mutable.{Specification, Before}
import spray.testkit.Specs2RouteTest
import spray.http.StatusCodes._
import scala.concurrent.duration._
import akka.actor.{ActorSystem, Props}
import edu.ufl.dos15.db._
import edu.ufl.dos15.crypto.Crypto._

class FriendListServiceSpec extends Specification with Specs2RouteTest with FriendListService with Before {
  import FriendListService._
  import FBMessage._

  implicit val routeTestTimeout = RouteTestTimeout(FiniteDuration(5, SECONDS))

  def actorRefFactory = system
  override val keyPair = RSA.generateKeyPair()

  val clienKeyPair = RSA.generateKeyPair()

  def before() = {
    val db = system.actorOf(Props[EncryptedDataDB], "db")
    db ! DBTestInsert("41", """{"name": "myfriends"}""".getBytes())
  }

  sequential

  "The FriendListService" should {

    "return OK for GET requests to /friends" in {
      Get("/friends") ~> friendListRoute ~> check {
        response.status should be equalTo OK
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

    "return success for PUT request to existed id with Json body" in {
      Put("/friends/41", FriendList(name=Some("myfriends"))) ~> friendListRoute ~> check {
        response.status should be equalTo OK
        response.entity should not be equalTo(None)
        val reply = responseAs[HttpSuccessReply]
        reply.success should be equalTo(true)
      }
    }

    "return success for PUT request to existed id with parameter" in {
      Put("/friends/41?ids=1234,2345,3456") ~> friendListRoute ~> check {
        response.status should be equalTo OK
        response.entity should not be equalTo(None)
        val reply = responseAs[HttpSuccessReply]
        reply.success should be equalTo(true)
      }
    }

    "return specific fileds for GET request to /friends/{id}?<fileds>" in {
      Get("/friends/41?fields=name,total_count") ~> friendListRoute ~> check {
        response.status should be equalTo OK
        response.entity should not be equalTo(None)
        val friends = responseAs[FriendList]
        friends.name.getOrElse("") === "myfriends"
        friends.total_count.getOrElse(0) should be equalTo(3)
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

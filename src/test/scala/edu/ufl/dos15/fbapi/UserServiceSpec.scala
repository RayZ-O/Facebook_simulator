package edu.ufl.dos15.fbapi

import org.specs2.mutable.{Specification, Before}
import spray.testkit.Specs2RouteTest
import spray.http.StatusCodes._
import scala.concurrent.duration._
import akka.actor.{ActorSystem, Props}
import edu.ufl.dos15.db._

class UserServiceSpec extends Specification with Specs2RouteTest with UserService with Before {
  import UserService._
  import FeedService._
  import FBMessage._

  implicit val routeTestTimeout = RouteTestTimeout(FiniteDuration(5, SECONDS))

  def actorRefFactory = system

  def before() = {
    val db = system.actorOf(Props[EncryptedDataDB], "db")
    db ! DBTestInsert("1", """{"email": "ruizhang1011@ufl.edu",
                               "gender": "male",
                               "first_name": "Rui",
                               "last_name": "Zhang"}""".getBytes())
  }

  sequential

  "The UserService" should {

    "return OK for GET requests to /user" in {
      Get("/user") ~> userRoute ~> check {
        response.status should be equalTo OK
      }
    }

    "return id for POST requests to /user" in {
      Post("/user", User(email=Some("ruizhang1011@ufl.edu"),
                     gender=Some("male"),
                     first_name=Some("Rui"),
                     last_name=Some("Zhang"))) ~> userRoute ~> check {
        response.status should be equalTo Created
        response.entity should not be equalTo(None)
        val reply = responseAs[HttpIdReply]
        reply.id.equals("") should be equalTo(false)
      }
    }

    "return all fileds for GET request to /user/{id}" in {
      Get("/user/1") ~> userRoute ~> check {
        response.status should be equalTo OK
        response.entity should not be equalTo(None)
        val user = responseAs[User]
        user === User(id=Some("1"),
                      email=Some("ruizhang1011@ufl.edu"),
                      gender=Some("male"),
                      first_name=Some("Rui"),
                      last_name=Some("Zhang"))
      }
    }

    "return success for PUT request to existed id" in {
      Put("/user/1", User(email=Some("rayzhang1011@gmail.com"))) ~> userRoute ~> check {
        response.status should be equalTo OK
        response.entity should not be equalTo(None)
        val reply = responseAs[HttpSuccessReply]
        reply.success should be equalTo(true)
      }
    }

    "return specific fileds for GET request to /user/{id}?<fileds>" in {
      Get("/user/1?fields=email,gender") ~> userRoute ~> check {
        response.status should be equalTo OK
        response.entity should not be equalTo(None)
        val user = responseAs[User]
        user.email.getOrElse("") === "rayzhang1011@gmail.com"
        user.gender.getOrElse("") === "male"
      }
    }

    "return id for POST requests to /user/{id}/feed" in {
      Post("/user/1/feed", Feed(message=Some("I am happy today"))) ~> userRoute ~> check {
        response.status should be equalTo Created
        response.entity should not be equalTo(None)
        val reply = responseAs[HttpIdReply]
        reply.id.equals("") should be equalTo(false)
      }
    }

    "return success for DELETE request to existed id" in {
      Delete("/user/1") ~> userRoute ~> check {
        response.status should be equalTo OK
        response.entity should not be equalTo(None)
        val reply = responseAs[HttpSuccessReply]
        reply.success should be equalTo(true)
      }
    }

    "return NotFound for GET request to non-existed id" in {
      Get("/user/2") ~> userRoute ~> check {
        response.status should be equalTo NotFound
      }
    }

    "return NotFound for PUT request to non-existed id" in {
      Put("/user/2", User(email=Some("rayzhang1011@gmail.com"))) ~> userRoute ~> check {
        response.status should be equalTo NotFound
      }
    }

    "return NotFound for DELETE request to non-existed id" in {
      Delete("/user/2") ~> userRoute ~> check {
        response.status should be equalTo NotFound
      }
    }
  }
}

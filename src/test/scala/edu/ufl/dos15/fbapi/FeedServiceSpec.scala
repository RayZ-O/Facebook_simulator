package edu.ufl.dos15.fbapi

import org.specs2.mutable.{Specification, Before}
import spray.testkit.Specs2RouteTest
import spray.http.StatusCodes._
import scala.concurrent.duration._
import akka.actor.{ActorSystem, Props}
import edu.ufl.dos15.db._

class FeedServiceSpec extends Specification with Specs2RouteTest with FeedService with Before {
  import FeedService._
  import FBMessage._

  implicit val routeTestTimeout = RouteTestTimeout(FiniteDuration(5, SECONDS))

  def actorRefFactory = system

  def before() = {
    val db = system.actorOf(Props[MockDB], "db")
    db ! DBTestInsert("31", """{"message": "I am happy today"}""")
  }

  sequential

  "The FeedService" should {

    "return OK for GET requests to /feed" in {
      Get("/feed") ~> feedRoute ~> check {
        response.status should be equalTo OK
      }
    }

    "return all fileds for GET request to /feed/{id}" in {
      Get("/feed/31") ~> feedRoute ~> check {
        response.status should be equalTo OK
        response.entity should not be equalTo(None)
        val feed = responseAs[Feed]
        feed === Feed(id=Some("31"),
                      message=Some("I am happy today")
                      )
      }
    }

    "return success for PUT request to existed id" in {
      Put("/feed/31", Feed(message=Some("I am happy yesterday"))) ~> feedRoute ~> check {
        response.status should be equalTo OK
        response.entity should not be equalTo(None)
        val reply = responseAs[HttpSuccessReply]
        reply.success should be equalTo(true)
      }
    }

    "return specific fileds for GET request to /feed/{id}?<fileds>" in {
      Get("/feed/31?fields=message") ~> feedRoute ~> check {
        response.status should be equalTo OK
        response.entity should not be equalTo(None)
        val feed = responseAs[Feed]
        feed.message.getOrElse("") === "I am happy yesterday"
      }
    }

    "return success for DELETE request to existed id" in {
      Delete("/feed/31") ~> feedRoute ~> check {
        response.status should be equalTo OK
        response.entity should not be equalTo(None)
        val reply = responseAs[HttpSuccessReply]
        reply.success should be equalTo(true)
      }
    }

    "return NotFound for GET request to non-existed id" in {
      Get("/feed/2") ~> feedRoute ~> check {
        response.status should be equalTo NotFound
      }
    }

    "return NotFound for PUT request to non-existed id" in {
      Put("/feed/2", Feed(message=Some("I am happy yesterday"))) ~> feedRoute ~> check {
        response.status should be equalTo NotFound
      }
    }

    "return NotFound for DELETE request to non-existed id" in {
      Delete("/feed/2") ~> feedRoute ~> check {
        response.status should be equalTo NotFound
      }
    }
  }
}

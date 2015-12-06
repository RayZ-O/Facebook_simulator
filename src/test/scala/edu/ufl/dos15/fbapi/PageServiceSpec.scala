package edu.ufl.dos15.fbapi

import org.specs2.mutable.{Specification, Before}
import spray.testkit.Specs2RouteTest
import spray.http.StatusCodes._
import scala.concurrent.duration._
import akka.actor.{ActorSystem, Props}
import edu.ufl.dos15.db._

class PageServiceSpec extends Specification with Specs2RouteTest with PageService with Before{
  import PageService._
  import FeedService._

  implicit val routeTestTimeout = RouteTestTimeout(FiniteDuration(5, SECONDS))

  def actorRefFactory = system

  def before() = {
    val db = system.actorOf(Props[MockDB], "db")
    db ! DBTestInsert("21", """{"name": "mypage"}""")
  }

  sequential

  "The PageService" should {

    "return OK for GET requests to /page" in {
      Get("/page") ~> pageRoute ~> check {
        response.status should be equalTo OK
      }
    }

    "return id for POST requests to /page" in {
      Post("/page", Page()) ~> pageRoute ~> check {
        response.status should be equalTo Created
        response.entity should not be equalTo(None)
        val reply = responseAs[HttpIdReply]
        reply.id.equals("") should be equalTo(false)
      }
    }

    "return all fileds for GET request to /page/{id}" in {
      Get("/page/21") ~> pageRoute ~> check {
        response.status should be equalTo OK
        response.entity should not be equalTo(None)
        val page = responseAs[Page]
        page === Page(id=Some("21"),
                      name=Some("mypage")
                      )
      }
    }

    "return success for PUT request to existed id" in {
      Put("/page/21", Page(name=Some("mypage"))) ~> pageRoute ~> check {
        response.status should be equalTo OK
        response.entity should not be equalTo(None)
        val reply = responseAs[HttpSuccessReply]
        reply.success should be equalTo(true)
      }
    }

    "return specific fileds for GET request to /page/{id}?<fileds>" in {
      Get("/page/21?fields=name") ~> pageRoute ~> check {
        response.status should be equalTo OK
        response.entity should not be equalTo(None)
        val page = responseAs[Page]
        page.name.getOrElse("") === "mypage"
      }
    }

    "return id for POST requests to /page/{id}/feed" in {
      Post("/page/21/feed", Feed(message=Some("I am happy today"))) ~> pageRoute ~> check {
        response.status should be equalTo Created
        response.entity should not be equalTo(None)
        val reply = responseAs[HttpIdReply]
        reply.id.equals("") should be equalTo(false)
      }
    }

    "return success for DELETE request to existed id" in {
      Delete("/page/21") ~> pageRoute ~> check {
        response.status should be equalTo OK
        response.entity should not be equalTo(None)
        val reply = responseAs[HttpSuccessReply]
        reply.success should be equalTo(true)
      }
    }

    "return NotFound for GET request to non-existed id" in {
      Get("/page/2") ~> pageRoute ~> check {
        response.status should be equalTo NotFound
      }
    }

    "return NotFound for PUT request to non-existed id" in {
      Put("/page/2", Page(name=Some("mypage"))) ~> pageRoute ~> check {
        response.status should be equalTo NotFound
      }
    }

    "return NotFound for DELETE request to non-existed id" in {
      Delete("/page/2") ~> pageRoute ~> check {
        response.status should be equalTo NotFound
      }
    }
  }
}

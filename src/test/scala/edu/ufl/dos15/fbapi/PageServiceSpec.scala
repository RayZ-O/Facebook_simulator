package edu.ufl.dos15.fbapi

import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest
import spray.http.StatusCodes._

class PageServiceSpec extends Specification with Specs2RouteTest with PageService {
    def actorRefFactory = system

    "The PageService" should {

    "return OK for GET requests to /page" in {
      Get("/page") ~> pageRoute ~> check {
        response.status should be equalTo OK
      }
    }


  }
}

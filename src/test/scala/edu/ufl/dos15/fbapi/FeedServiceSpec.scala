package edu.ufl.dos15.fbapi

import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest
import spray.http.StatusCodes._

class FeedServiceSpec extends Specification with Specs2RouteTest with FeedService {
    def actorRefFactory = system

    "The FeedService" should {

    "return OK for GET requests to the root path" in {
      Get() ~> feedRoute ~> check {
        response.status should be equalTo OK
        response.entity should not be equalTo(None)
      }
    }


  }
}

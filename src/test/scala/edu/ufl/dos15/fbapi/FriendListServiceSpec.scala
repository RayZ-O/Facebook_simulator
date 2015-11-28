package edu.ufl.dos15.fbapi

import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest
import spray.http.StatusCodes._

class FriendListServiceSpec extends Specification with Specs2RouteTest with FriendListService {
    def actorRefFactory = system

    "The FriendListService" should {

    "return OK for GET requests to /friends" in {
      Get("/friends") ~> friendListRoute ~> check {
        response.status should be equalTo OK
        response.entity should not be equalTo(None)
      }
    }


  }
}

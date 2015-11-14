package edu.ufl.dos15.fbapi

import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest
import spray.http.StatusCodes._

class AlbumServiceSpec extends Specification with Specs2RouteTest with AlbumService {
    def actorRefFactory = system

    "The AlbumService" should {

    "return OK for GET requests to the root path" in {
      Get() ~> albumRoute ~> check {
        response.status should be equalTo OK
        response.entity should not be equalTo(None)
      }
    }


  }
}

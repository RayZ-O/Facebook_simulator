package edu.ufl.dos15.fbapi

import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest
import spray.http.StatusCodes._
import spray.http._
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization.{read, write}

class UserServiceSpec extends Specification with Specs2RouteTest with UserService {
    import Json4sProtocol._
    import UserService._
    def actorRefFactory = system
    var id = ""
    def before = {
      id = RyDB.insert("""{"email": "ruizhang1011@ufl.edu",
                           "gender": "male",
                           "first_name": "Rui",
                           "last_name": "Zhang"}""")
      println(id)
    }

    "The UserService" should {

    "return OK for GET requests to /user" in {
      Get("/user") ~> userRoute ~> check {
        response.status should be equalTo OK
      }
    }

    "return id for POST requests to /user" in {
      Post("/user", HttpEntity(ContentTypes.`application/json`,
          write(User(email=Some("ruizhang1011@ufl.edu"),
                     gender=Some("male"),
                     first_name=Some("Rui"),
                     last_name=Some("Zhang"))))) ~> userRoute ~> check {
        response.status should be equalTo Created
        response.entity should not be equalTo(None)
        val user = responseAs[User]
        user.id.equals("") should be equalTo(false)
      }
    }

    "return all fileds for GET request to /user/{id}" in {
      Get(s"/user/$id") ~> userRoute ~> check {
        response.status should be equalTo OK
        val user = read[User](responseAs[String])
        user === User(id=Some(id),
                      email=Some("ruizhang1011@ufl.edu"),
                      gender=Some("male"),
                      first_name=Some("Rui"),
                      last_name=Some("Zhang"))
      }
    }

    "return success for PUT request to existed id" in {
      Put(s"/user/$id", HttpEntity(MediaTypes.`application/json`,
          write(User(email=Some("rayzhang1011@gmail.com"))))) ~> userRoute ~> check {
        response.status should be equalTo OK
        val json = parse(responseAs[String])
        (json \ "success") should not be equalTo(JBool(true))
      }
    }

    "return NotFound for PUT request to non-existed id" in {
      Put("/user/1", HttpEntity(MediaTypes.`application/json`,
          write(User(email=Some("rayzhang1011@gmail.com"))))) ~> userRoute ~> check {
        response.status should be equalTo NotFound
      }
    }

    "return specific fileds for GET request to /user/{id}?<fileds>" in {
      Get(s"/user/$id?fields=email,gender") ~> userRoute ~> check {
        response.status should be equalTo OK
        val json = parse(responseAs[String])
        json \ "email" === "rayzhang1011@gmail.com"
        json \ "gender" === "male"
      }
    }




  }
}

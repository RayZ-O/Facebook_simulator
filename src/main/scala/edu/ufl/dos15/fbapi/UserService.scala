package edu.ufl.dos15.fbapi

import scala.concurrent.duration.Duration
import spray.routing.Route
import spray.routing.directives.CachingDirectives._
import spray.http.MediaTypes
import spray.http.HttpResponse
import spray.http.StatusCodes
import spray.routing.HttpService
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization.write
import spray.http.HttpEntity
import spray.http.ContentTypes


object UserService {
    import PageService.Page

    case class User (
        id: Option[String] = None,           // The id of this person's user account
        email: Option[String] = None,        // The person's primary email address
        gender: Option[String] = None,       // The gender selected by this person, male or female
        first_name: Option[String] = None,   // The person's first name
        last_name: Option[String] = None,    // The person's last name
        verified: Boolean = false,    // Indicates whether the account has been verified
        middle_name: Option[String] = None,  // The person's middle name
        birthday: Option[String] = None,  // The person's birthday. MM/DD/YYYY
        link: Option[String] = None,     // A link to the person's Timeline
        locale: Option[String] = None,   // The person's locale
        timezone: Option[Float] = None,  // The person's current timezone offset from UTC
        location: Option[Page] = None)       // The person's current location
}

trait UserService extends HttpService {
    import Json4sProtocol._
    import UserService._

    val userCache = routeCache(maxCapacity = 1000, timeToIdle = Duration("30 min"))

    val userRoute: Route = respondWithMediaType(MediaTypes.`application/json`) {
        (path("user") & get) {
          complete(StatusCodes.OK)
        } ~
        (path("user") & post) {  // creates a user
          entity(as[User]) { user =>
            detach() {
                val id = RyDB.insert(write(user))
                import org.json4s.JsonDSL._
                complete(HttpResponse(StatusCodes.Created,
                    HttpEntity(compact(render("id" -> id)))))
            }
          }
        } ~
        pathPrefix("user" / Segment) { id => // gets infomation about a user
          get {
            parameter('fields.?) { fields =>
                detach() {
                  RyDB.get(id) match {
                    case s if s != null =>
                      val json = parse(s)
                      val result = fields match {
                        case Some(fileds) =>
                          val query = fields.get.split(",")
                          import org.json4s.JsonDSL._
                          query.foldLeft(JObject()) { (res, name) =>
                            (res ~ (name -> json \ name)) }
                        case None => json
                      }
//                      cache(userCache) {
                        complete(StatusCodes.OK, result)
//                      }
                    case _ =>
                      complete(StatusCodes.NotFound)
               }
             }
           }
         }~
         put { // update a user
           entity(as[User]) { values =>
             detach() {
               RyDB.update(id, write(values)) match {
                 case true =>
                   import org.json4s.JsonDSL._
                   complete(StatusCodes.OK, render("success" -> true))
                 case false =>
                   complete(StatusCodes.NotFound)
               }

             }
           }
         } ~
         delete { // delete a user
           RyDB.delete(id) match {
             case true =>
               import org.json4s.JsonDSL._
               complete(StatusCodes.OK, render("success" -> true))
             case false =>
               complete(StatusCodes.NotFound)
           }
         }
      }
    }
}

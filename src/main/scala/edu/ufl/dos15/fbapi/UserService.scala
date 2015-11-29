package edu.ufl.dos15.fbapi

import scala.concurrent.duration.Duration
import spray.routing.Route
import spray.routing.directives.CachingDirectives._
import spray.routing.HttpService
import spray.http.StatusCodes
import spray.routing.RequestContext
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization.write
import akka.actor.Props

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

trait UserService extends HttpService with PerRequestFactory {
    import Json4sProtocol._
    import UserService._

    val userCache = routeCache(maxCapacity = 1000, timeToIdle = Duration("30 min"))

    val userRoute: Route = {
      (path("user") & get) {
        complete(StatusCodes.OK)
      } ~
      (path("user") & post) {  // creates a user
        entity(as[User]) { user =>
          ctx => handleRequest(ctx, Post(user))
        }
      } ~
      pathPrefix("user" / Segment) { id => // gets infomation about a user
        get {
          parameter('fields.?) { fields =>
            ctx => handleRequest(ctx, Get(id, fields))
          }
        }~
        put { // update a user
          entity(as[User]) { values =>
            ctx => handleRequest(ctx, Put(id, values))
          }
        } ~
        delete { // delete a user
          ctx => handleRequest(ctx, Delete(id))
        }
      }
    }
}

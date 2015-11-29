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

object PageService {
    case class Page (
        id: String,                          // Page ID. No access token is required to access this field
        category: Option[String] = None,     // The Page's category. e.g. Product/Service, Computers/Technology
        description: Option[String] = None,  // The description of the Page
        link: Option[String] = None,         // The Page's Facebook URL
        name: Option[String] = None,         // The name of the Page
        username: Option[String] = None,     // The alias of the Page
        likes: Int = 0,                      // The number of users who like the Page
        location: Option[Location] = None,   // The location of this place. Applicable to all Places
        parent_page: Option[Page] = None)                 // Parent Page for this Page

    case class Location (
        city: String,
        country: String,
        latitude: Float,
        longitude: Float,
        located_in: String, //The parent location if this location is located within another location
        name: String,
        region: String,
        state: String,
        street: String,
        zip: String)

}

trait PageService extends HttpService with PerRequestFactory {
    import Json4sProtocol._
    import PageService._

    val pageCache = routeCache(maxCapacity = 1000, timeToIdle = Duration("30 min"))

    val pageRoute: Route = {
      (path("page") & get) {
        complete(StatusCodes.OK)
      } ~
      (path("page") & post) {  // creates a user
        entity(as[Page]) { page =>
          ctx => handleRequest(ctx, Post(page))
        }
      } ~
      pathPrefix("page" / Segment) { id => // gets infomation about a user
        get {
          parameter('fields.?) { fields =>
            ctx => handleRequest(ctx, Get(id, fields))
          }
        }~
        put { // update a user
          entity(as[Page]) { values =>
            ctx => handleRequest(ctx, Put(id, values))
          }
        } ~
        delete { // delete a user
          ctx => handleRequest(ctx, Delete(id))
        }
      }
    }
}

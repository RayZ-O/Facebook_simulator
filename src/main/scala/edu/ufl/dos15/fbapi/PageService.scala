package edu.ufl.dos15.fbapi

import scala.concurrent.duration.Duration
import spray.routing.Route
import spray.routing.directives.CachingDirectives._
import spray.http.MediaTypes
import spray.http.HttpResponse
import spray.routing.HttpService

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

trait PageService extends HttpService {

    val pageCache = routeCache(maxCapacity = 1000, timeToIdle = Duration("30 min"))

    val pageRoute: Route = respondWithMediaType(MediaTypes.`application/json`) {
        pathPrefix("page" / Segment) { id =>
          get {
            cache(pageCache) {
              complete("Get")
              // TODO Get Request
            }
          } ~
          post {
            complete("Post")
            // TODO Post Request
          } ~
          delete {
            complete("Deleted")
            // TODO Delete Request
          }
        }
    }
}

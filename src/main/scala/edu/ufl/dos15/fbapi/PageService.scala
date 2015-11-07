package edu.ufl.dos15.fbapi

import scala.concurrent.duration.Duration
import spray.routing.Route
import spray.routing.directives.CachingDirectives._
import spray.http.MediaTypes
import spray.http.HttpResponse
import spray.routing.HttpService

object PageService {
    case class Page (
        id: String,           // Page ID. No access token is required to access this field
        category: String,     // The Page's category. e.g. Product/Service, Computers/Technology
        description: String,  // The description of the Page
        link: String,         // The Page's Facebook URL
        name: String,         // The name of the Page
        username: String,     // The alias of the Page. e.g, for www.facebook.com/platform the username is 'platform'
        checkins: Int,        // Number of checkins at a place represented by a Page
        likes: Int,           // The number of users who like the Page
        location: Location,   // The location of this place. Applicable to all Places
        parent_page: Page){    // Parent Page for this Page
    }

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
        zip: String){
    }
}

trait PageService extends HttpService {

    val pageCache = routeCache(maxCapacity = 1000, timeToIdle = Duration("30 min"))

    val pageRoute: Route = respondWithMediaType(MediaTypes.`application/json`) {
        pathPrefix("group") {
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

package edu.ufl.dos15.fbapi

import scala.concurrent.duration.Duration
import spray.routing.Route
import spray.routing.directives.CachingDirectives._
import spray.http.MediaTypes
import spray.http.HttpResponse
import spray.routing.HttpService

import AlbumService.Album
import UserService.User
import PageService.Page

object PhotoService {
    case class Photo (
        id: String,              // The photo ID
        album: Album,            // The album this photo is in
        created_time: String,    // The time this photo was published
        can_delete: Boolean,     // A boolean indicating if the viewer can delete the photo
        // can_tag: Boolean,     // A boolean indicating if the viewer can tag the photo
        from: User,              // The profile (user or page) that uploaded this photo
        height: Int,             // The height of this photo in pixels
        width: Int,              // The width of this photo in pixels
        link: String,            // A link to the photo on Facebook
        name: String,            // The user-provided caption given to this photo.
        page_story_id: String,   // ID of the page story this corresponds to.
        updated_time: String,    // The last time the photo was updated
        location: Page,          // Location associated with the photo, if any
        picture: String)         // Link to the 100px wide representation of this photo
}

trait PhotoService extends HttpService {

    val photoCache = routeCache(maxCapacity = 1000, timeToIdle = Duration("30 min"))

    val photoRoute: Route = respondWithMediaType(MediaTypes.`application/json`) {
        pathPrefix("photo") {
          get {
            cache(photoCache) {
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

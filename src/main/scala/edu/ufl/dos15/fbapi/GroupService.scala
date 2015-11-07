package edu.ufl.dos15.fbapi

import scala.concurrent.duration.Duration
import spray.routing.Route
import spray.routing.directives.CachingDirectives._
import spray.http.MediaTypes
import spray.http.HttpResponse
import spray.routing.HttpService

import edu.ufl.dos15.fbapi.PhotoService.Photo
import edu.ufl.dos15.fbapi.UserService.User
import edu.ufl.dos15.fbapi.PageService.Page

object GroupService {
    case class Group (
        id: String,             // The group ID
        cover: Photo,           // Information about the group's cover photo.
        description: String,    // A brief description of the group.
        email: String,          // The email address to upload content to the group.
        icon: String,           // The URL for the group's icon.
        link: String,           // The group's website.
        name: String,           // The name of the group.
        owner: User,            // The profile that created this group.
        parent: Group,          // The parent of this group, if it exists.
        privacy: String,        // The privacy setting of the group. CLOSED, OPEN or SECRET
        updated_time: String,     // The last time the group was updated
        member_request_count: Int // The number of pending member requests.
    )
}

trait GroupService extends HttpService {

    val groupCache = routeCache(maxCapacity = 1000, timeToIdle = Duration("30 min"))

    val groupRoute: Route = respondWithMediaType(MediaTypes.`application/json`) {
        pathPrefix("group") {
          get {
            cache(groupCache) {
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

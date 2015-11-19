package edu.ufl.dos15.fbapi

import scala.concurrent.duration.Duration
import spray.routing.Route
import spray.routing.directives.CachingDirectives._
import spray.http.MediaTypes
import spray.http.HttpResponse
import spray.routing.HttpService

import PageService.Page
import UserService.User

object AlbumService {
    case class Album (
        id: String,              // The album ID.
        can_upload: Boolean,     // Whether the viewer can upload photos to this album.
        count: Int,              // Number of photos in this album.
        cover_photo: String,     // The ID of the album's cover photo.
        created_time: Long,      // The time the album was initially created.
        description: String,     // The description of the album.
        from: User,              // The profile that created the album
        link: String,            // A link to this album on Facebook
        name: String,            // The title of the album
        place: Page,             // The place associated with this album
        privacy: String,         // The privacy settings for the album
        album_type: AlbumType.Value, // The type of the album
        updated_time: Long)      // The last time the album was updated

    object AlbumType extends Enumeration {
         type AlbumType = Value
         val APP,
             COVER,
             PROFILE,
             MOBILE,
             WALL,
             NORMAL,
             ALBUM = Value
    }
}

trait AlbumService extends HttpService {

    val albumCache = routeCache(maxCapacity = 1000, timeToIdle = Duration("30 min"))

    val albumRoute: Route = respondWithMediaType(MediaTypes.`application/json`) {
        pathPrefix("album") {
          get {
            cache(albumCache) {
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

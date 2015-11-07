package edu.ufl.dos15.fbapi

import scala.concurrent.duration.Duration
import spray.routing.Route
import spray.routing.directives.CachingDirectives._
import spray.http.MediaTypes
import spray.http.HttpResponse
import spray.routing.HttpService

trait FriendListService extends HttpService {

    val friendListCache = routeCache(maxCapacity = 1000, timeToIdle = Duration("30 min"))

    val friendListRoute: Route = respondWithMediaType(MediaTypes.`application/json`) {
        pathPrefix("friendlist") {
          get {
            cache(friendListCache) {
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

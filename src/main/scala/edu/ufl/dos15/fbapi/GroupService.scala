package edu.ufl.dos15.fbapi

import scala.concurrent.duration.Duration
import spray.routing.Route
import spray.routing.directives.CachingDirectives._
import spray.http.MediaTypes
import spray.http.HttpResponse
import spray.routing.HttpService

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

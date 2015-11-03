import scala.concurrent.duration.Duration
import spray.routing.HttpService
import spray.routing.Route
import spray.routing.directives.CachingDirectives._
import spray.http.MediaTypes
import spray.http.HttpResponse

trait FacebookService extends HttpService {
    val simpleCache = routeCache(maxCapacity = 1000, timeToIdle = Duration("30 min"))

    val rest: Route = respondWithMediaType(MediaTypes.`application/json`) {
        path("user") {
          get {
            cache(simpleCache) {
              complete{
                   "Get"
              }
            }
          } ~
          post {
            complete {
              "Post"
            }
          } ~
          delete {
            complete {
              "Deleted"
            }
          }
        }
    }
}

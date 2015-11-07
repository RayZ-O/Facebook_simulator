package edu.ufl.dos15.fbapi

import scala.concurrent.duration.Duration
import spray.routing.Route
import spray.routing.directives.CachingDirectives._
import spray.http.MediaTypes
import spray.http.HttpResponse
import spray.routing.HttpService

object FriendListService {
    case class FriendList (
        id: String,                         //The friend list ID
        name: String,                       //The name of the friend list
        list_type: FriendListType.Value,    //The type of the friend list
        owner: String)                      //The owner of the friend list

        object FriendListType extends Enumeration {
            type FriendListType = Value
            val CLOSE_FRIENDS,
                ACQUAINTANCES,
                RESTRICTED,
                USER_CREATED,
                EDUCATION,
                WORK,
                CURRENT_CITY,
                FAMILY = Value
        }
}

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

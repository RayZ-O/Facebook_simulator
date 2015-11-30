package edu.ufl.dos15.fbapi

import scala.concurrent.duration.Duration
import spray.routing.Route
import spray.routing.directives.CachingDirectives._
import spray.routing.HttpService
import spray.http.StatusCodes

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

trait FriendListService extends HttpService with PerRequestFactory with Json4sProtocol {
    import FriendListService._

    val friendListCache = routeCache(maxCapacity = 1000, timeToIdle = Duration("30 min"))

    val friendListRoute: Route = {
      (path("friends") & get) {
        complete(StatusCodes.OK)
      } ~
      (path("friends") & post) {  // creates a friend list
        entity(as[FriendList]) { friendList =>
          ctx => handleRequest(ctx, Post(friendList))
        }
      } ~
      pathPrefix("friends" / Segment) { id => // gets infomation about a friend list
        get {
          parameter('fields.?) { fields =>
            ctx => handleRequest(ctx, Get(id, fields))
          }
        }~
        put { // update a friends in a friend list
          entity(as[FriendList]) { values =>
            ctx => handleRequest(ctx, Put(id, values))
          }
        } ~
        delete { // delete a friend list
          ctx => handleRequest(ctx, Delete(id))
        }
      }
    }
}

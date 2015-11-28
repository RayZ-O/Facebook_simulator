package edu.ufl.dos15.fbapi

import scala.concurrent.duration.Duration
import spray.routing.Route
import spray.routing.directives.CachingDirectives._
import spray.http.MediaTypes
import spray.http.StatusCodes
import spray.routing.HttpService

import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization.write

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
    import Json4sProtocol._
    import FriendListService._

    val friendListCache = routeCache(maxCapacity = 1000, timeToIdle = Duration("30 min"))

    val friendListRoute: Route = respondWithMediaType(MediaTypes.`application/json`) {
          (path("friends") & get) {
            complete(StatusCodes.OK)
          } ~
          (path("friends") & post) {  // creates a user
            entity(as[FriendList]) { friendList =>
              detach() {
                val id = RyDB.insert(write(friendList))
                import org.json4s.JsonDSL._
                complete(StatusCodes.Created, render("id" -> id))
              }
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

package edu.ufl.dos15.fbapi

import scala.concurrent.duration.Duration
import spray.routing.Route
import spray.routing.directives.CachingDirectives._
import spray.routing.HttpService
import spray.http.StatusCodes

import edu.ufl.dos15.fbapi.actor._

object FriendListService {
  import UserService.User

  case class FriendList (
    id: Option[String] = None,                         // The friend list ID
    name: Option[String] = None,                       // The name of the friend list
    list_type: Option[FriendListType.Value] = None,    // The type of the friend list
    owner: Option[String] = None,                      // The owner of the friend list
    data: Option[List[String]] = None,                 // Friends in the friend list
    total_count: Option[Int] = None) {                 // Total number of friends

    def addOwner(id: String) = {
      this.copy(owner = Some(id))
    }
  }

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
  import FBMessage._

  val friendListCache = routeCache(maxCapacity = 1000, timeToIdle = Duration("30 min"))

  val friendListRoute: Route = {
    (path("friends") & get) {
      complete(StatusCodes.OK)
    } ~
    pathPrefix("friends" / Segment) { id => // gets infomation about a friend list
      get {
        ctx => handleRequest(ctx, Fetch(id))
      } ~
      put { // update a friends in a friend list
        parameter('ids) { ids =>
          ctx => handleRequest(ctx, PutList(id, ids))
        } ~
        entity(as[String]) { values =>
          ctx => handleRequest(ctx, Update(id, values))
        }
      } ~
      delete { // delete a friend list
        parameter('ids) { ids =>
          ctx => handleRequest(ctx, DeleteList(id, ids))
        } ~
        {
          ctx => handleRequest(ctx, Delete(id))
        }
      }
    }
  }
}

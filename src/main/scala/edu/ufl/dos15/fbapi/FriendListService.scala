package edu.ufl.dos15.fbapi

import scala.concurrent.duration.Duration
import spray.routing.{Route, RequestContext, HttpService}
import spray.routing.directives.CachingDirectives._
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

trait FriendListService extends HttpService with Json4sProtocol with RequestActorFactory with Authenticator {
  import FriendListService._
  import FBMessage._

  val friendListCache = routeCache(maxCapacity = 1000, timeToIdle = Duration("30 min"))

  val friendListRoute: Route = {
    (path("friends") & get) {
      complete(StatusCodes.OK)
    } ~
    (path("friends") & post) {  // creates a friend list
        authenticate(tokenAuthenticator) { uid =>
          entity(as[EncryptedData]) { ed =>
            ctx => handle[FriendListActor](ctx, PostData(uid, ed, "friend"))
          }
        }
    } ~
    (path("friends" / "me") & get) {  // creates a friend list
        authenticate(tokenAuthenticator) { uid =>
          ctx => handle[FriendListActor](ctx, GetFriendList(uid))
        }
    } ~
    pathPrefix("friends" / Segment) { objId => // gets infomation about a friend list
      authenticate(tokenAuthenticator) { uid =>
        get {
          path("list") {
            ctx => handle[FriendListActor](ctx, FetchList(objId))
          } ~
          path("info") {
            ctx => handle[DataStoreActor](ctx, GetKey(uid, objId, "friend"))
          }
        } ~
        put { // update a friends in a friend list
          parameter('ids) { ids =>
            ctx => handle[FriendListActor](ctx, PutList(objId, ids))
          } ~
          entity(as[EncryptedData]) { ed =>
            ctx => handle[DataStoreActor](ctx, UpdateData(uid, objId, ed, "friend"))
          }
        } ~
        delete { // delete a friend list
          parameter('ids) { ids =>
            ctx => handle[FriendListActor](ctx, DeleteList(objId, ids))
          } ~
          pathEnd {
            ctx => handle[FriendListActor](ctx, Delete(objId, Some(uid)))
          }
        }
      }
    }
  }
}

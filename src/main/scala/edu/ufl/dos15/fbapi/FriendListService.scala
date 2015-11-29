package edu.ufl.dos15.fbapi

import scala.concurrent.duration.Duration
import spray.routing.Route
import spray.routing.directives.CachingDirectives._
import spray.routing.HttpService
import spray.http.StatusCodes
import spray.routing.RequestContext
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization.write
import akka.actor.Props


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
    val db = actorRefFactory.actorSelection("/db")

    val friendListRoute: Route = {
      (path("friends") & get) {
        complete(StatusCodes.OK)
      } ~
      (path("friends") & post) {  // creates a user
        entity(as[FriendList]) { friendList =>
          ctx => handleRequest(ctx, Post(friendList))
        }
      } ~
      pathPrefix("friends" / Segment) { id => // gets infomation about a user
        get {
          parameter('fields.?) { fields =>
            ctx => handleRequest(ctx, Get(id, fields))
          }
        }~
        put { // update a user
          entity(as[FriendList]) { values =>
            ctx => handleRequest(ctx, Put(id, values))
          }
        } ~
        delete { // delete a user
          ctx => handleRequest(ctx, Delete(id))
        }
      }
    }

    def handleRequest(ctx: RequestContext, msg: Message) = {
      actorRefFactory.actorOf(Props(classOf[PerRequestActor], ctx, db, msg))
    }
}

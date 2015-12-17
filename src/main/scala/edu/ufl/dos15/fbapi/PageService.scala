package edu.ufl.dos15.fbapi

import scala.concurrent.duration.Duration
import spray.routing.{Route, RequestContext, HttpService}
import spray.routing.directives.CachingDirectives._
import spray.http.StatusCodes
import edu.ufl.dos15.fbapi.actor._

object PageService {
  case class Page (
    id: Option[String] = None,           // Page ID. No access token is required to access this field
    category: Option[String] = None,     // The Page's category. e.g. Product/Service, Computers/Technology
    description: Option[String] = None,  // The description of the Page
    link: Option[String] = None,         // The Page's Facebook URL
    name: Option[String] = None,         // The name of the Page
    likes: Option[Int] = None,           // The number of users who like the Page
    location: Option[Location] = None)   // The location of this place. Applicable to all Places

  case class Location (
    city: Option[String] = None,
    country: Option[String] = None,
    latitude: Option[Float] = None,
    longitude: Option[Float] = None,
    name: Option[String] = None,
    region: Option[String] = None,
    state: Option[String] = None,
    street: Option[String] = None,
    zip: Option[String] = None)
}

trait PageService extends HttpService with RequestActorFactory with Json4sProtocol with Authenticator {
  import PageService._
  import FeedService._
  import FriendListService._
  import FBMessage._

  val pageCache = routeCache(maxCapacity = 1000, timeToIdle = Duration("30 min"))

  val pageRoute: Route = {
    (path("page") & get) {
      complete(StatusCodes.OK)
    } ~
    (path("page") & post) {  // creates a page
      authenticate(tokenAuthenticator) { uid =>
        entity(as[EncryptedData]) { ed =>
          ctx => handle[DataStoreActor](ctx, PostData(uid, ed, "profile"))
        }
      }
    } ~
    pathPrefix("page" / Segment) { objId => // gets infomation about a page
      authenticate(tokenAuthenticator) { uid =>
        get {
          ctx => handle[DataStoreActor](ctx, GetKey(uid, objId, "profile"))
        } ~
        put { // update a page
          entity(as[UpdatedData]) { ud =>
            ctx => handle[DataStoreActor](ctx, ud)
          }
        } ~
        delete { // delete a page
          ctx => handle[DataStoreActor](ctx, Delete(objId))
        }
      }
    }
  }
}

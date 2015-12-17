package edu.ufl.dos15.fbapi

import scala.concurrent.duration.Duration
import spray.routing.{Route, RequestContext, HttpService}
import spray.routing.directives.CachingDirectives._
import spray.http.StatusCodes
import edu.ufl.dos15.fbapi.actor._

object UserService {
  import PageService.Page

case class User (
  id: Option[String] = None,           // The id of this person's user account
  email: Option[String] = None,        // The person's primary email address
  gender: Option[String] = None,       // The gender selected by this person, male or female
  first_name: Option[String] = None,   // The person's first name
  last_name: Option[String] = None,    // The person's last name
  verified: Option[Boolean] = None,    // Indicates whether the account has been verified
  middle_name: Option[String] = None,  // The person's middle name
  birthday: Option[String] = None,  // The person's birthday. MM/DD/YYYY
  link: Option[String] = None,     // A link to the person's Timeline
  locale: Option[String] = None,   // The person's locale
  timezone: Option[Float] = None,  // The person's current timezone offset from UTC
  location: Option[Page] = None)       // The person's current location
}

trait UserService extends HttpService with RequestActorFactory with Json4sProtocol with Authenticator {
  import UserService._
  import FeedService._
  import FriendListService._
  import FBMessage._

  val userCache = routeCache(maxCapacity = 1000, timeToIdle = Duration("30 min"))

  val userRoute: Route = {
    (path("user") & get) {
      complete(StatusCodes.OK)
    } ~
    (path("user") & post) {  // creates a user
        authenticate(tokenAuthenticator) { uid =>
          entity(as[EncryptedData]) { ed =>
            ctx => handle[DataStoreActor](ctx, PostData(uid, ed, "profile"))
          }
        }
      } ~
    pathPrefix("user" / Segment) { objId => // gets infomation about a user
      authenticate(tokenAuthenticator) { uid =>
        get {
          ctx => handle[DataStoreActor](ctx, GetKey(uid, objId, "profile"))
        } ~
        put { // update a user
          entity(as[Array[Byte]]) { value =>
            ctx => handle[DataStoreActor](ctx, Update(objId, value))
          }
        } ~
        delete { // delete a user
          ctx => handle[DataStoreActor](ctx, Delete(objId))
        }
      }
    }
  }
}

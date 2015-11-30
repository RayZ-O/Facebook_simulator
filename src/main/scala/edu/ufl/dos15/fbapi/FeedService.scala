package edu.ufl.dos15.fbapi

import scala.concurrent.duration.Duration
import spray.routing.Route
import spray.routing.directives.CachingDirectives._
import spray.routing.HttpService
import spray.http.StatusCodes

object FeedService {
  import UserService._
  import PageService._

  case class Feed (
    id: Option[String] = None,            // The post ID
    created_time: Option[String] = None,  // The time the post was initially published.
    from: Option[User] = None,            // Information about the profile that posted the message.
    is_hidden: Option[Boolean] = None,    // If this post is marked as hidden (applies to Pages only).
    link: Option[String] = None,          // The link attached to this post.
    message: Option[String] = None,       // The status message in the post.
    name: Option[String] = None,          // The name of the link.
    object_id: Option[String] = None,     // The ID of any uploaded photo or video attached to the post.
    place: Option[Page] = None,           // Any location information attached to the post.
    privacy: Option[Privacy] = None,      // The privacy settings of the post.
    source: Option[String] = None,        // A URL to any Flash movie or video file attached to the post.
    status_type: Option[StatusType.Value] = None,  // Description of the type of a status update.
    to: Option[Array[User]] = None,        // Profiles mentioned or targeted in this post.
    feed_type: Option[FeedType.Value] = None,    // A string indicating the object type of this post.
    updated_time: Option[String] = None,    // The time of the last change to this post, or the comments on it.
    with_tags: Option[Array[User]] = None)  // Profiles tagged as being 'with' the publisher of the post.

  case class Privacy (
    description: String,  //Text that describes the privacy settings
    value: PrivacyType.Value, // The actual privacy setting.
    allow: List[String],  // A ID list of users and friendlists (if any) that can see the post.
    deny: List[String])  // A ID list of users and friendlists (if any) that cannot see the post.

  object PrivacyType extends Enumeration {
    type PrivacyType = Value
    val EVERYONE,
        ALL_FRIENDS,
        FRIENDS_OF_FRIENDS,
        SELF,
        CUSTOM = Value
  }

  object StatusType extends Enumeration {
    type StatusType = Value
    val MOBILE_STATUS_UPDATE,
        CREATED_NOTE,
        ADDED_PHOTOS,
        ADDED_VIDEO,
        SHARED_STORY,
        CREATED_GROUP,
        CREATED_EVENT,
        WALL_POST,
        APP_CREATED_STORY,
        PUBLISHED_STORY,
        TAGGED_IN_PHOTO,
        APPROVED_FRIEND = Value
  }

  object FeedType extends Enumeration {
     type FeedType = Value
     val LINK,
         STATUS,
         PHOTO,
         VIDEO,
         OFFER,
         EVENT = Value
  }
}

trait FeedService extends HttpService with PerRequestFactory with Json4sProtocol {
  import FeedService._

  val feedCache = routeCache(maxCapacity = 1000, timeToIdle = Duration("30 min"))

  val feedRoute: Route = {
    (path("feed") & get) {
      complete(StatusCodes.OK)
    } ~
    (path("feed") & post) {  // creates a post(feed)
      entity(as[Feed]) { feed =>
        ctx => handleRequest(ctx, Post(feed))
      }
    } ~
    pathPrefix("feed" / Segment) { id => // gets infomation about a post(feed)
      get {
        parameter('fields.?) { fields =>
          ctx => handleRequest(ctx, Get(id, fields))
        }
      }~
      put { // update a post(feed)
        entity(as[Feed]) { values =>
          ctx => handleRequest(ctx, Put(id, values))
        }
      } ~
      delete { // delete a post(feed)
        ctx => handleRequest(ctx, Delete(id))
      }
    }
  }
}

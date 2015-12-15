package edu.ufl.dos15.fbapi

import scala.concurrent.duration.Duration
import spray.routing.{RequestContext, HttpService, Route}
import spray.routing.directives.CachingDirectives._
import spray.http.StatusCodes
import java.util.Calendar

import edu.ufl.dos15.fbapi.actor._

object FeedService {
  import UserService._
  import PageService._

  case class Feed (
    id: Option[String] = None,            // The post ID
    created_time: Option[String] = None,  // The time the post was initially published.
    from: Option[String] = None,            // Information about the profile that posted the message.
    is_hidden: Option[Boolean] = None,    // If this post is marked as hidden (applies to Pages only).
    link: Option[String] = None,          // The link attached to this post.
    message: Option[String] = None,       // The status message in the post.
    name: Option[String] = None,          // The name of the link.
    place: Option[Page] = None,           // Any location information attached to the post.
    privacy: Option[PrivacyType.Value] = None,      // The privacy settings of the post.
    source: Option[String] = None,        // A URL to any Flash movie or video file attached to the post.
    to: Option[Array[User]] = None,        // Profiles mentioned or targeted in this post.
    feed_type: Option[FeedType.Value] = None,    // A string indicating the object type of this post.
    updated_time: Option[String] = None,    // The time of the last change to this post, or the comments on it.
    with_tags: Option[Array[User]] = None) { // Profiles tagged as being 'with' the publisher of the post.

    def addFromAndCreatedTime(id: String) = {
      this.copy(from = Some(id), created_time = Some(Calendar.getInstance.getTime.toString()))
    }

    def addUpdatedTime() = {
      this.copy(updated_time = Some(Calendar.getInstance.getTime.toString()))
    }
  }

  object PrivacyType extends Enumeration {
    type PrivacyType = Value
    val EVERYONE,                   // 0
        ALL_FRIENDS,                // 1
        FRIENDS_OF_FRIENDS,         // 2
        SELF = Value                // 3
  }

  object FeedType extends Enumeration {
     type FeedType = Value
     val LINK,
         STATUS,
         PHOTO,
         VIDEO,
         OFFER = Value
  }
}

trait FeedService extends HttpService with RequestActorFactory with Json4sProtocol {
  import FeedService._
  import FBMessage._

  val feedCache = routeCache(maxCapacity = 1000, timeToIdle = Duration("30 min"))

  val feedRoute: Route = {
    (path("feed") & get) {
      complete(StatusCodes.OK)
    } ~
    (path("feed" / "new") & get) {
      ctx => handle[DataStoreActor](ctx, GetNewPosts())
    } ~
    pathPrefix("feed" / Segment) { id => // gets infomation about a post(feed)
      get {
        parameter('fields.?) { fields =>
          ctx => handle[DataStoreActor](ctx, Get(id, fields))
        }
      } ~
      put { // update a post(feed)
        entity(as[String]) { values =>
          ctx => handle[DataStoreActor](ctx, Put(id, values))
        }
      } ~
      delete { // delete a post(feed)
        ctx => handle[DataStoreActor](ctx, Delete(id))
      }
    }
  }
}

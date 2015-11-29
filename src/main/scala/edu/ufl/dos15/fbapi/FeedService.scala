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

object FeedService {
    import UserService.User
    import PageService.Page

    case class Feed (
        id: String,            // The post ID
        created_time: String,  // The time the post was initially published.
        from: User,            // Information about the profile that posted the message.
        is_hidden: Boolean,    // If this post is marked as hidden (applies to Pages only).
        link: String,          // The link attached to this post.
        message: String,       // The status message in the post.
        name: String,          // The name of the link.
        object_id: String,     // The ID of any uploaded photo or video attached to the post.
        place: Page,           // Any location information attached to the post.
        privacy: Privacy,       // The privacy settings of the post.
        source: String,        // A URL to any Flash movie or video file attached to the post.
        status_type: StatusType.Value,  // Description of the type of a status update.
        story: String,         // Text from stories not intentionally generated by users
        to: Array[User],       // Profiles mentioned or targeted in this post.
        feed_type: FeedType.Value,    // A string indicating the object type of this post.
        updated_time: String,    // The time of the last change to this post, or the comments on it.
        with_tags: Array[User])  // Profiles tagged as being 'with' the publisher of the post.

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

trait FeedService extends HttpService {
    import Json4sProtocol._
    import FeedService._

    val feedCache = routeCache(maxCapacity = 1000, timeToIdle = Duration("30 min"))
    val db = actorRefFactory.actorSelection("/db")

    val feedRoute: Route = {
      (path("page") & get) {
        complete(StatusCodes.OK)
      } ~
      (path("page") & post) {  // creates a user
        entity(as[Feed]) { feed =>
          ctx => handleRequest(ctx, Post(feed))
        }
      } ~
      pathPrefix("page" / Segment) { id => // gets infomation about a user
        get {
          parameter('fields.?) { fields =>
            ctx => handleRequest(ctx, Get(id, fields))
          }
        }~
        put { // update a user
          entity(as[Feed]) { values =>
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

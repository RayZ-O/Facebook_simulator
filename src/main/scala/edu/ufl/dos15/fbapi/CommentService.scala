package edu.ufl.dos15.fbapi

import scala.concurrent.duration.Duration
import spray.routing.Route
import spray.routing.directives.CachingDirectives._
import spray.http.MediaTypes
import spray.http.HttpResponse
import spray.routing.HttpService

import edu.ufl.dos15.fbapi.UserService.User

object CommentService {
    case class Comment(
        id: String,                   // The comment ID
        // attachment: StoryAttachment,  // Link or photo attached to the comment
        can_comment: Boolean,         // Whether the viewer can reply to this comment
        can_remove: Boolean,          // Whether the viewer can remove this comment
        can_hide: Boolean,            // Whether the viewer can hide this comment
        can_like: Boolean,            // Whether the viewer can like this comment
        comment_count: Int,           // Number of replies to this comment
        created_time: String,         // The time this comment was made
        from: User,                   // The person that made this comment
        like_count: Int,              // Number of times this comment was liked
        message: String,              // The comment text
        // message_tags: Array[Any],       // An array of Profiles tagged in message.
        parent_object: Any,           // Parent object this comment was made on
        parent_comment: Comment,      // For comment replies, this the comment that this is a reply to
        user_likes: Boolean)          // Whether the viewer has liked this comment
}

trait CommentService extends HttpService {

    val commentCache = routeCache(maxCapacity = 1000, timeToIdle = Duration("30 min"))

    val commentRoute: Route = respondWithMediaType(MediaTypes.`application/json`) {
        pathPrefix("comment") {
          get {
            cache(commentCache) {
              complete("Get")
              // TODO Get Request
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
}

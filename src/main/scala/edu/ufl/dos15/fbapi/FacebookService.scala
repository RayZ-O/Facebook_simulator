package edu.ufl.dos15.fbapi

import scala.concurrent.duration.Duration
import spray.routing.HttpService
import spray.routing.Route
import spray.routing.directives.CachingDirectives._
import spray.http.MediaTypes
import spray.http.HttpResponse

trait FacebookService extends HttpService with AlbumService with CommentService
    with FriendListService with GroupService with PageService with PhotoService
    with FeedService with UserService {

    val FacebookAPIRoute = albumRoute ~         // A photo album
                           commentRoute ~       // A single comment
                           friendListRoute ~    // This represents a user's friend list on Facebook
                           groupRoute ~         // A Facebook group
                           pageRoute ~          // A Facebook page
                           photoRoute ~         // This represents a Photo on Facebook
                           feedRoute ~          // An individual entry in a feed.
                           userRoute            // A single user node
}

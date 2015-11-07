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

    val FacebookAPIRoute = albumRoute ~
                           commentRoute ~
                           friendListRoute ~
                           groupRoute ~
                           pageRoute ~
                           photoRoute ~
                           feedRoute ~
                           userRoute
}

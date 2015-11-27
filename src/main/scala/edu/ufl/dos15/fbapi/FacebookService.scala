package edu.ufl.dos15.fbapi

import scala.concurrent.duration.Duration
import spray.routing.HttpService
import spray.routing.Route
import spray.routing.directives.CachingDirectives._
import spray.http.MediaTypes
import spray.http.HttpResponse
import spray.httpx.Json4sSupport
import org.json4s._
import org.json4s.ext.EnumSerializer


object Json4sProtocol extends Json4sSupport {
    import FriendListService._
    implicit def json4sFormats: Formats = DefaultFormats + new EnumSerializer(FriendListType)
}

trait FacebookService extends HttpService
                      with FriendListService
                      with PageService
                      with FeedService
                      with UserService {

    val FacebookAPIRoute = friendListRoute ~    // This represents a user's friend list on Facebook
                           pageRoute ~          // A Facebook page
                           feedRoute ~          // An individual entry in a feed.
                           userRoute            // A single user node
}

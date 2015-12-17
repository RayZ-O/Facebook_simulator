package edu.ufl.dos15.fbapi

import spray.routing.Route
import spray.httpx.Json4sSupport
import org.json4s._
import org.json4s.ext.EnumSerializer

trait FacebookService extends AuthService
                      with UserService
                      with PageService
                      with FeedService
                      with FriendListService {

    val FacebookAPIRoute = authRoute ~
                           userRoute ~          // A single user node
                           pageRoute ~          // A Facebook page
                           feedRoute ~          // An individual entry in a feed.
                           friendListRoute      // This represents a user's friend list on Facebook
}

trait Json4sProtocol extends Json4sSupport {
    implicit def json4sFormats: Formats = DefaultFormats +
                                          new EnumSerializer(FriendListService.FriendListType) +
                                          new EnumSerializer(FeedService.PrivacyType) +
                                          new EnumSerializer(FeedService.FeedType)
}

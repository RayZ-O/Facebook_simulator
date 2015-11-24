package edu.ufl.dos15.fbapi

import java.util.UUID

import scala.concurrent.duration.Duration
import spray.routing.Route
import spray.routing.directives.CachingDirectives._
import spray.http.MediaTypes
import spray.http.HttpResponse
import spray.httpx.Json4sSupport
import spray.routing.HttpService

import org.json4s._
import org.json4s.native.JsonMethods._

import PageService.Page

object UserService {
    case class User (
        id: Option[String] = None,           // The id of this person's user account
        email: String,        // The person's primary email address
        gender: String,       // The gender selected by this person, male or female
        first_name: String,   // The person's first name
        last_name: String,    // The person's last name
        verified: Boolean = false,    // Indicates whether the account has been verified
        middle_name: Option[String] = None,  // The person's middle name
        birthday: Option[String] = None,  // The person's birthday. MM/DD/YYYY
        link: Option[String] = None,     // A link to the person's Timeline
        locale: Option[String] = None,   // The person's locale
        timezone: Option[Float] = None,  // The person's current timezone offset from UTC
        location: Option[Page] = None)       // The person's current location
}

object Json4sProtocol extends Json4sSupport {
    implicit def json4sFormats: Formats = DefaultFormats
}

trait UserService extends HttpService {
    import Json4sProtocol._
    import UserService._

    val userCache = routeCache(maxCapacity = 1000, timeToIdle = Duration("30 min"))
    import scala.collection.mutable.HashMap
    val db = new HashMap[String, String]
    val userRoute: Route = respondWithMediaType(MediaTypes.`application/json`) {
        pathPrefix("user") {
          get {  // gets infomation about a user
            path(Segment) { id =>
              println(id)
              parameter('fields.?) { fields =>
                val json = parse(db(id))
                cache(userCache) {
                  complete {
                    if (fields.isEmpty) {
                        json
                    } else {
                        val query = fields.get.split(",")
                        import org.json4s.JsonDSL._
                        query.foldLeft(JObject()) { (res, name) =>
                          (res ~ (name -> json \ name))
                        }
                    }
                  }
                }
              }
            }
          } ~
          post {  // creates a user
            entity(as[JObject]) { jobject =>
                val id = java.util.UUID.randomUUID().toString()
                import org.json4s.JsonDSL._
                val userJvalue = ("id" -> id) ~ jobject
                db += id -> compact(render(userJvalue))
                println(pretty(render(userJvalue)))
                complete(SuccessID(id))
            }
          } ~
          put {  // update a user
             path(Segment) { id =>
               parameterSeq { params =>
                 import org.json4s.JsonDSL._
                 val fields = params.foldLeft(JObject()) { (res, param) =>
                                (res ~ (param._1 -> param._2))
                              }
                 val updated = parse(db(id)) merge fields
                 db += id -> compact(render(updated))
                 complete(Success(true))
               }
             }
          } ~
          delete {  // delete a user
            path(Segment) { id =>
              db -= id
              complete(Success(true))
            }
          }
        }
    }
}

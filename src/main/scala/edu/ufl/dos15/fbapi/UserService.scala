package edu.ufl.dos15.fbapi

import scala.concurrent.duration.Duration
import spray.routing.Route
import spray.routing.directives.CachingDirectives._
import spray.http.MediaTypes
import spray.http.HttpResponse
import spray.routing.HttpService

import edu.ufl.dos15.fbapi.PageService.Page

object UserService {
    case class User (
        id: String,           // The id of this person's user account
        first_name: String,   // The person's first name
        last_name: String,    // The person's last name
        middle_name: String,  // The person's middle name
        name: String,         // The person's full name
        gender: String,       // The gender selected by this person, male or female
        birthday: String,     // The person's birthday. MM/DD/YYYY
        email: String,        // The person's primary email address
        link: String,         // A link to the person's Timeline
        location: Page,       // The person's current location
        locale: String,       // The person's locale
        timezone: Float,      // The person's current timezone offset from UTC
        verified: Boolean,    // Indicates whether the account has been verified
        hometown: Page,       // The person's hometown
        education: List[EducationExperience])  // The person's education

    case class EducationExperience(
        classes: List[Experience],  // Classes taken
        concentration: List[Page],  // Facebook Pages representing subjects studied
        degree: Page,               // The Facebook Page for the degree obtained
        school: Page,               // The Facebook Page for this school
        education_type: String,     // The type of educational institution
        with_who: List[User],       // People tagged who went to school with this person
        year: Page)                 // Facebook Page for the year this person graduated

    case class Experience (
        id: String,
        description: String,
        name: String,
        from: User,
        with_who: List[User])
}

trait UserService extends HttpService {

    val userCache = routeCache(maxCapacity = 1000, timeToIdle = Duration("30 min"))

    val userRoute: Route = respondWithMediaType(MediaTypes.`application/json`) {
        pathPrefix("photo") {
          get {
            cache(userCache) {
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

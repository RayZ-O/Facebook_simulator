package edu.ufl.dos15.fbapi

import spray.routing.HttpService
import spray.routing.Route
import edu.ufl.dos15.fbapi.actor.AuthActorCreator

trait AuthService extends HttpService with AuthActorCreator with Json4sProtocol {
  val authRoute: Route = {
    (path("register") & post) {
      entity(as[Register]) { reg =>
        ctx => handleAuth(ctx, reg)
      }
    } ~
    (path("login") & get) {
      parameter('id) { id =>
        ctx => handleAuth(ctx, GetNonce(id))
      }
    }
    (path("login") & post) {
      entity(as[PassWdAuth]) { cred =>
        ctx => handleAuth(ctx, cred)
      }
    }
  }
}

package edu.ufl.dos15.fbapi

import spray.routing.{HttpService, RequestContext, Route}
import edu.ufl.dos15.fbapi.actor._

trait AuthService extends HttpService with RequestActorFactory with Json4sProtocol {
  import FBMessage._

  val authRoute: Route = {
    (path("register") & post) {
      entity(as[RegisterCred]) { reg =>
        ctx => handle[AuthActor](ctx, reg)
      }
    } ~
    (path("login") & get) {
      parameter('id) { id =>
        ctx => handle[AuthActor](ctx, GetNonce(id))
      }
    }
    (path("login" / "password") & post) {
      entity(as[PassWdAuth]) { cred =>
        ctx => handle[AuthActor](ctx, cred)
      }
    } ~
    (path("login" / "pubkey") & post) {
      entity(as[CheckNonce]) { cn =>
        ctx => handle[AuthActor](ctx, cn)
      }
    }
  }
}

package edu.ufl.dos15.fbapi

import spray.routing.{HttpService, RequestContext, Route}
import edu.ufl.dos15.fbapi.actor._

trait AuthService extends HttpService with RequestActorFactory with Json4sProtocol with Authenticator {
  import FBMessage._

  val authRoute: Route = {
    (path("register") & post) {
      entity(as[RegisterCred]) { reg =>
        ctx => handle[AuthActor](ctx, reg, Some(keyPair.getPrivate().getEncoded))
      }
    } ~
    (path("login" / Segment) & get) { id =>
      ctx => handle[AuthActor](ctx, GetNonce(id))
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

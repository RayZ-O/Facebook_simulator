package edu.ufl.dos15.fbapi

import scala.concurrent.Future
import scala.concurrent.duration._
import akka.util.Timeout
import akka.pattern.ask
import spray.routing.authentication.ContextAuthenticator
import spray.routing.{Rejection, AuthenticationFailedRejection, HttpService}
import spray.routing.AuthenticationFailedRejection._
import edu.ufl.dos15.crypto.Crypto._
import java.security.PrivateKey

trait Authenticator {
  this: HttpService =>
  def priKey: PrivateKey
  case object AuthorizationTimeoutRejection extends Rejection

  import FBMessage._
  implicit def executionContext = actorRefFactory.dispatcher
  val tokenAuthenticator: ContextAuthenticator[String] = { ctx =>
     ctx.request.headers.find(_.name == "ACCESS-TOKEN").map(_.value) match {
       case Some(encrypted) =>
         val authDB = actorRefFactory.actorSelection("/user/auth-db")
         implicit val timeout = Timeout(2.seconds)
         val token = RSA.decrypt(encrypted, priKey)
         val f = authDB ? TokenAuth(token)
         f.map {
           case reply: DBStrReply =>
             reply.content match {
               case Some(id) => Right(id)
               case None => Left(AuthenticationFailedRejection(CredentialsRejected, ctx.request.headers))
             }
         }.recover {
           case _ => Left(AuthorizationTimeoutRejection)
         }
       case None =>
         Future(Left(AuthenticationFailedRejection(CredentialsMissing, ctx.request.headers)))
    }
  }
}

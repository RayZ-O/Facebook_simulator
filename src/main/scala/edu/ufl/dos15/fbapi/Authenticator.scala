package edu.ufl.dos15.fbapi

import java.util.Base64
import java.security.KeyPair
import scala.concurrent.Future
import scala.concurrent.duration._
import akka.util.Timeout
import akka.pattern.ask
import spray.routing.authentication.ContextAuthenticator
import spray.routing.{Rejection, AuthenticationFailedRejection, HttpService}
import spray.routing.AuthenticationFailedRejection._
import edu.ufl.dos15.crypto.Crypto._

trait Authenticator  {
  this: HttpService =>
  import FBMessage._
  case object AuthorizationTimeoutRejection extends Rejection

  val keyPair: KeyPair

  implicit def executionContext = actorRefFactory.dispatcher
  val tokenAuthenticator: ContextAuthenticator[String] = { ctx =>
     ctx.request.headers.find(_.name == "ACCESS-TOKEN").map(_.value) match {
       case Some(encrypted) =>
         val authDB = actorRefFactory.actorSelection("/user/auth-db")
         val token = RSA.decrypt(encrypted.getBytes(), keyPair.getPrivate())
         implicit val timeout = Timeout(2.seconds)
         val f = authDB ? TokenAuth(new String(token))
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

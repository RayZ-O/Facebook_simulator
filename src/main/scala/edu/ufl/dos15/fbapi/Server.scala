package edu.ufl.dos15.fbapi

import akka.actor.Actor
import edu.ufl.dos15.crypto.Crypto.RSA

class Server extends Actor with FacebookService {
    override val keyPair = RSA.generateKeyPair()
    // abstract value members of trait HttpService
    def actorRefFactory = context
    // executing Route in FacebookService .
    def receive = runRoute(FacebookAPIRoute)
}

package edu.ufl.dos15.fbapi

import java.io.BufferedOutputStream
import java.io.FileOutputStream
import akka.actor.Actor
import edu.ufl.dos15.crypto.Crypto.RSA

class Server extends Actor with FacebookService {
    override val keyPair = RSA.generateKeyPair()
    val bos = new BufferedOutputStream(new FileOutputStream("server.pem", false))
    Stream.continually(bos.write(keyPair.getPublic().getEncoded()))
    bos.close()
    // abstract value members of trait HttpService
    def actorRefFactory = context
    // executing Route in FacebookService .
    def receive = runRoute(FacebookAPIRoute)
}

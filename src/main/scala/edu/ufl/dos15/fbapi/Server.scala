package edu.ufl.dos15.fbapi

import akka.actor.Actor

class Server extends Actor with FacebookService {
    // abstract value members of trait HttpService
    def actorRefFactory = context
    // executing Route in FacebookService .
    def receive = runRoute(FacebookAPIRoute)
}

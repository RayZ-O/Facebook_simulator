import akka.actor.Actor

class Server extends Actor with FacebookService {
    def receive = runRoute(rest)
}

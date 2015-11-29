import akka.actor.{Actor, ActorLogging}

case class Get(id: String)
case class Insert(value: String)
case class Update(id: String, value: String)
case class Delete(id: String)

case class DBReply(success: Boolean, content: Option[String] = None)

class MockDB extends Actor with ActorLogging {
    import scala.collection.mutable.HashMap
    private var db = new HashMap[String, String]
    private var count = 0;

    def receive: Receive = {
        case Get(id) =>
            db.get(id) match {
                case Some(value) =>
                    sender ! DBReply(true, Some(value))
                case None =>
                    sender ! DBReply(false)
            }

        case Insert(value) =>
            count += 1
            val id = System.currentTimeMillis().toString + count
            db += (id -> value)
            sender ! DBReply(true, Some(id))

        case Update(id: String, value: String) =>
            db.get(id) match {
                case Some(value) =>
                    db += (id -> value)
                    sender ! DBReply(true)
                case None =>
                    sender ! DBReply(false)
            }

        case Delete(id: String) =>
            if (db.contains(id)) {
                db -= id
                sender ! DBReply(true)
            } else {
                sender ! DBReply(false)
            }
    }
}

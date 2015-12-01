package edu.ufl.dos15.fbapi

import akka.actor.{Actor, ActorLogging}


class MockDB extends Actor with ActorLogging {
    import scala.collection.mutable.HashMap
    private var db = new HashMap[String, String]
    import scala.collection.mutable.ListBuffer
    private var posts = new ListBuffer[String]
    private var count = 0;

    def receive: Receive = {
      case Fetch(id) =>
//        log.info(s"fetch $id")
        db.get(id) match {
          case Some(value) =>
            sender ! DBReply(true, Some(value))
          case None =>
            sender ! DBReply(false)
        }

      case GetNewPosts() =>
        println(posts.take(10).toList)
        sender ! PostReply(posts.take(10).toList)

      case Insert(value) =>
//        log.info(s"insert $id -> $value")
        sender ! DBReply(true, Some(insert(value)))

      case EdgeInsert(id, value, post) =>
        if (db.contains(id)) {
          val insertedId = insert(value)
          sender ! DBReply(true, Some(insertedId))
          if (post == true) {
            insertedId +=: posts
          }
        } else {
          sender ! DBReply(false)
        }

      case Update(id, newValue) =>
//        log.info(s"update $id to $newValue")
        if (db.contains(id)) {
          db += (id -> newValue)
          sender ! DBReply(true)
        } else {
          sender ! DBReply(false)
        }

      case Delete(id) =>
//        log.info(s"delete $id")
        if (db.contains(id)) {
          db -= id
          sender ! DBReply(true)
        } else {
          sender ! DBReply(false)
        }

      case DBTestInsert(id, value) =>
//        log.info(s"test insert $id -> $value, count = $count")
        if (!db.contains(id)) {
          count += 1
          db += (id -> value)
        }
    }

    def insert(value: String) = {
      count += 1
      val id = System.currentTimeMillis().toString + count
      db += (id -> value)
      id
    }
}

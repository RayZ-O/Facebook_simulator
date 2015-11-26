package edu.ufl.dos15.fbapi

object RyDB {
    import scala.collection.mutable.HashMap
    private var db = new HashMap[String, String]
    private var count = 0;

    def get(id: String): String = if (db.contains(id)) db(id) else null

    def insert(value: String): String = {
        count += 1
        val id = System.currentTimeMillis().toString + count
        db += (id -> value)
        id
    }

    def update(id: String, value: String): Boolean = {
        if (!db.contains(id)) {
            false
        } else {
            db += (id -> value)
            true
        }
    }

    def delete(id: String): Boolean = {
        if (!db.contains(id)) {
            false
        } else {
            db -= id
            true
        }
    }
}

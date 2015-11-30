package edu.ufl.dos15.fbapi

sealed trait Message
// server message
case class Get(id: String, params: Option[String]) extends Message
case class Post(obj: AnyRef) extends Message
case class EdgePost(id: String, obj: AnyRef) extends Message
case class Put(id: String, obj: AnyRef) extends Message
case class PutList(id: String, ids: String) extends Message
case class HttpSuccessReply(success: Boolean) extends Message
case class HttpIdReply(id: String) extends Message
case class Error(message: String) extends Message
// databse message
case class Fetch(id: String) extends Message
case class Insert(value: String) extends Message
case class EdgeInsert(id: String, value: String) extends Message
case class Update(id: String, value: String) extends Message
case class DBReply(success: Boolean, content: Option[String] = None) extends Message
case class DBTestInsert(id: String, value: String) extends Message
// common
case class Delete(id: String) extends Message
case class DeleteMul(id: String, ids: Array[String]) extends Message

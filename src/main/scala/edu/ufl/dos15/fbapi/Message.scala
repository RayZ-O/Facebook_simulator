package edu.ufl.dos15.fbapi

sealed trait Message
case class Get(id: String, params: Option[String]) extends Message
case class Post(obj: AnyRef) extends Message
case class Put(id: String, obj: AnyRef) extends Message
case class Fetch(id: String) extends Message
case class Insert(value: String) extends Message
case class Update(id: String, value: String) extends Message
case class Delete(id: String) extends Message

case class DBReply(success: Boolean, content: Option[String] = None) extends Message
case class HttpSuccessReply(success: Boolean) extends Message
case class HttpIdReply(id: String) extends Message

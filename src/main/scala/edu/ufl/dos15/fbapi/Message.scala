package edu.ufl.dos15.fbapi

object FBMessage {
  sealed trait Message
  // server message
  import scala.collection.mutable.HashMap
  case class EncryptedData(data: Array[Byte], iv: Array[Byte], keys: HashMap[String, Array[Byte]]) extends Message
  case class Get(id: String, params: Option[String]) extends Message
  case class GetNewPosts() extends Message
  case class Post(obj: AnyRef) extends Message
  case class EdgePost(id: String, obj: AnyRef, post: Boolean) extends Message
  case class Put(id: String, info: String) extends Message
  case class PutList(id: String, ids: String) extends Message
  case class DeleteList(id: String, ids: String) extends Message
  case class HttpSuccessReply(success: Boolean) extends Message
  case class HttpIdReply(id: String) extends Message
  case class HttpTokenReply(token: String) extends Message
  case class HttpListReply(list: List[String]) extends Message
  case class HttpDataReply(data: Array[Byte], iv: Array[Byte], key: Array[Byte]) extends Message
  case class Error(message: String) extends Message
  // databse message
  case class Fetch(id: String) extends Message
  case class Insert(value: String) extends Message
  case class EdgeInsert(id: String, value: String, post: Boolean) extends Message
  case class InsertPost(id: String) extends Message
  case class Update(id: String, value: String) extends Message
  case class UpdateMul(id: String, ls: List[String]) extends Message
  case class DBReply(success: Boolean, content: Option[String] = None) extends Message
  case class FetchReply(success: Boolean, content: Option[String] = None) extends Message
  case class InsertReply(success: Boolean, id: Option[String] = None) extends Message
  case class UpdateReply(success: Boolean) extends Message
  case class DeleteReply(success: Boolean) extends Message
  case class PostReply(posts: List[String]) extends Message
  case class DBTestInsert(id: String, value: String) extends Message
  // common
  case class FindCommon(id1: String, id2: String) extends Message
  case class Delete(id: String) extends Message
  case class DeleteMul(id: String, ids: List[String]) extends Message
  case object Tick extends Message
  // auth message
  case class Register(username: String, passwd: String, pubKey: String) extends Message
  case class GetNonce(id: String) extends Message
  case class PassWdAuth(username: String, passwd: String) extends Message
  case class TokenAuth(token: String) extends Message

}

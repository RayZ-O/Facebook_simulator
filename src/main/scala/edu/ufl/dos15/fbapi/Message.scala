package edu.ufl.dos15.fbapi

object FBMessage {
  sealed trait Message
  // server message
  import scala.collection.mutable.HashMap
  case class EncryptedData(data: Array[Byte], iv: Array[Byte], 
      keys: HashMap[String, Array[Byte]]) extends Message
  case class Get(id: String, params: Option[String]) extends Message
  case class GetNewPosts() extends Message
  case class Post(obj: AnyRef) extends Message
  case class EdgePost(id: String, obj: AnyRef, post: Boolean) extends Message
  case class Put(id: String, info: String) extends Message
  case class PutList(id: String, ids: String) extends Message
  case class DeleteList(id: String, ids: String) extends Message
  case class Publish(ownerId: String, ownerKey: Array[Byte], objId: String, iv: Array[Byte], 
      keys: HashMap[String, Array[Byte]], pType: String) extends Message
  case class HttpSuccessReply(success: Boolean) extends Message
  case class HttpIdReply(id: String) extends Message
  case class HttpTokenReply(token: String) extends Message
  case class HttpListReply(list: List[String]) extends Message
  case class HttpDataReply(data: Array[Byte], iv: Array[Byte], key: Array[Byte]) extends Message
  case class Error(message: String) extends Message
  // databse message
  case class Fetch(id: String) extends Message
  case class InsertBytes(value: Array[Byte]) extends Message
  case class InsertStr(value: String) extends Message
  case class Update(id: String, value: Array[Byte]) extends Message
  case class UpdateMul(id: String, ls: List[String]) extends Message
  case class DBStrReply(success: Boolean, content: Option[String] = None) extends Message
  case class DBBytesReply(success: Boolean, content: Option[Array[Byte]] = None) extends Message
  case class DBTestInsert(id: String, value: Array[Byte]) extends Message
  // common
  case class FindCommon(id1: String, id2: String) extends Message
  case class Delete(id: String) extends Message
  case class DeleteMul(id: String, ids: List[String]) extends Message
  case object Tick extends Message
  // auth message
  case class Register(username: String, passwd: String, pubKey: Array[Byte]) extends Message
  case class GetNonce(id: String) extends Message
  case class CheckNonce(nonce: String, signature: Array[Byte]) extends Message
  case class PassWdAuth(username: String, passwd: String) extends Message
  case class TokenAuth(token: String) extends Message
  case class GetPubKey(id: String) extends Message

}

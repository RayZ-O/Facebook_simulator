package edu.ufl.dos15.fbapi

object FBMessage {
  sealed trait Message
  // server message
  import scala.collection.mutable.HashMap
  case class EncryptedData(data: Array[Byte],
                           iv: Array[Byte],
                           keys: HashMap[String, Array[Byte]]) extends Message
  case class UpdatedData(id: String,
                         data: Array[Byte],
                         iv: Array[Byte]) extends Message
  case class PostData(id: String, ed: EncryptedData, pType: String) extends Message
  case class GetKey(ownerId: String, objId: String, pType: String) extends Message
  case class GetFriendList(ownerId: String) extends Message
  case class PullFeed(id: String, start: Int) extends Message
  case class GetSelfPost(id: String) extends Message
  case class PutList(id: String, ids: String) extends Message
  case class DeleteList(id: String, ids: String) extends Message
  case class Publish(ownerId: String, objId: String, iv: Array[Byte],
      keys: HashMap[String, Array[Byte]], pType: String) extends Message
  case class PublishSelf(ownerId: String, iv: Array[Byte], key: Array[Byte]) extends Message
  case class HttpSuccessReply(success: Boolean) extends Message
  case class HttpIdReply(id: String) extends Message
  case class HttpListReply(list: List[String]) extends Message
  case class HttpDataReply(data: Array[Byte],
                           iv: Option[Array[Byte]] = None,
                           key: Option[Array[Byte]] = None) extends Message
  case class Error(message: String) extends Message
  // databse message
  case class Fetch(id: String) extends Message
  case class FetchList(id: String) extends Message
  case class InsertBytes(value: Array[Byte]) extends Message
  case class InsertStr(value: String) extends Message
  case class InsertList(ownerId: String, listId: String) extends Message
  case class Update(id: String, value: Array[Byte]) extends Message
  case class UpdateMul(id: String, ls: List[String]) extends Message
  case class DBSuccessReply(success: Boolean) extends Message
  case class DBStrReply(success: Boolean,
                        content: Option[String] = None,
                        key: Option[Array[Byte]] = None) extends Message
  case class DBBytesReply(success: Boolean,
                          content: Option[Array[Byte]] = None,
                          key: Option[Array[Byte]] = None) extends Message
  case class DBListReply(success: Boolean, list: Option[List[String]] = None) extends Message
  case class DBCredReply(success: Boolean,
                         iv: Option[Array[Byte]] = None,
                         key: Option[Array[Byte]] = None) extends Message
  case class DBTestInsert(id: String, value: Array[Byte]) extends Message
  case class DBTestToken(id: String, token: String) extends Message
  // common
  case class FindCommon(id1: String, id2: String) extends Message
  case class Delete(objId: String, ownerId: Option[String] = None) extends Message
  case class DeleteMul(id: String, ids: List[String]) extends Message
  case object Tick extends Message
  // auth message
  case class RegisterUser(data: Array[Byte], iv: Array[Byte], key: Array[Byte], 
      pubKey: Array[Byte]) extends Message
  case class Register(pubKey: Array[Byte]) extends Message
  case class GetNonce(id: String) extends Message
  case class CheckNonce(nonce: String, signature: Array[Byte]) extends Message
  case class TokenAuth(token: String) extends Message
  case class TokenCred(token: Array[Byte]) extends Message
  case class GetPubKey(id: String) extends Message

}

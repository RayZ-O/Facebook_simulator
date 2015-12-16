package edu.ufl.dos15.crypto

import java.util.Base64
import javax.crypto.KeyGenerator
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec
import java.security.KeyFactory
import java.security.SecureRandom
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.security.KeyPairGenerator
import java.security.Signature
import com.typesafe.config.ConfigFactory
import scala.util.Try

object Crypto {
  private val random = new SecureRandom();
  val config = ConfigFactory.load()
  val nonceBytes = Try(config.getInt("crypto.nonce-bytes")).getOrElse(12)
  val tokenBytes = Try(config.getInt("crypto.token-bytes")).getOrElse(12)

  def generateNonce() = {
    val nonce = new Array[Byte](nonceBytes)
    random.nextBytes(nonce)
    nonce.map("%02X".format(_)).mkString
  }

  def generateToken() = {
    val token = new Array[Byte](tokenBytes)
    random.nextBytes(token)
    token.map("%02X".format(_)).mkString
  }

  object AES {
    def generateKey(): SecretKey = {
      val keyGen = KeyGenerator.getInstance("AES")
      keyGen.init(128)
      keyGen.generateKey()
    }

    def generateIv(): IvParameterSpec = {
      val iv = new Array[Byte](16)
      random.nextBytes(iv)
      new IvParameterSpec(iv)
    }

    def decodeKey(bytes: Array[Byte]): SecretKey = {
      new SecretKeySpec(Base64.getDecoder().decode(bytes), "AES")
    }

    def encrypt(str: String, secKey: SecretKey, iv: IvParameterSpec): Array[Byte] = {
      encrypt(str.getBytes("UTF-8"), secKey, iv)
    }

    def encrypt(data: Array[Byte], secKey: SecretKey, iv: IvParameterSpec): Array[Byte] = {
      val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
      cipher.init(Cipher.ENCRYPT_MODE, secKey, iv)
      cipher.doFinal(data)
    }

    def decrypt(str: String, secKey: SecretKey, iv: IvParameterSpec): Array[Byte] = {
      decrypt(str.getBytes("UTF-8"), secKey, iv)
    }

    def decrypt(data: Array[Byte], secKey: SecretKey, iv: IvParameterSpec): Array[Byte] = {
      val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
      cipher.init(Cipher.DECRYPT_MODE, secKey, iv)
      cipher.doFinal(data)
    }
  }

  object RSA {
    def generateKeyPair(): KeyPair = {
      val keyGen = KeyPairGenerator.getInstance("RSA")
      keyGen.initialize(1024);
      keyGen.generateKeyPair()
    }

    def decodePubKey(bytes: Array[Byte]): PublicKey = {
      val spec = new X509EncodedKeySpec(bytes)
      val factory = KeyFactory.getInstance("RSA")
      factory.generatePublic(spec)
    }

    def decodePriKey(bytes: Array[Byte]): PrivateKey = {
      val spec = new PKCS8EncodedKeySpec(bytes)
      val factory = KeyFactory.getInstance("RSA")
      factory.generatePrivate(spec)
    }

    def encrypt(data: String, pubKey: Array[Byte]): Array[Byte] = {
      encrypt(data.getBytes("UTF-8"), decodePubKey(pubKey))
    }

    def encrypt(data: Array[Byte], pubKey: Array[Byte]): Array[Byte] = {
      encrypt(data, decodePubKey(pubKey))
    }

    def encrypt(data: String, pubKey: PublicKey): Array[Byte] = {
      encrypt(data.getBytes("UTF-8"), pubKey)
    }

    def encrypt(data: Array[Byte], pubKey: PublicKey): Array[Byte] = {
      val cipher = Cipher.getInstance("RSA")
      cipher.init(Cipher.ENCRYPT_MODE, pubKey)
      cipher.doFinal(data)
    }

    def decrypt(data: Array[Byte], priKey: Array[Byte]): Array[Byte] = {
      decrypt(data, decodePriKey(priKey))
    }

    def decrypt(data: Array[Byte], priKey: PrivateKey): Array[Byte] = {
      val cipher = Cipher.getInstance("RSA")
      cipher.init(Cipher.DECRYPT_MODE, priKey)
      cipher.doFinal(data)
    }

    def sign(data: String, priKey: PrivateKey): Array[Byte] = {
      sign(data.getBytes("UTF-8"), priKey)
    }

    def sign(data: Array[Byte], priKey: PrivateKey): Array[Byte] = {
      val signer = Signature.getInstance("SHA256withRSA")
      signer.initSign(priKey)
      signer.update(data)
      signer.sign
    }

    def verify(data: String, signature: Array[Byte], pubKey: Array[Byte]): Boolean = {
      verify(data.getBytes("UTF-8"), signature, decodePubKey(pubKey))
    }

    def verify(data: Array[Byte], signature: Array[Byte], pubKey: Array[Byte]): Boolean = {
      verify(data, signature, decodePubKey(pubKey))
    }

    def verify(data: Array[Byte], signature: Array[Byte], pubKey: PublicKey): Boolean = {
      val verifier = Signature.getInstance("SHA256withRSA")
      verifier.initVerify(pubKey)
      verifier.update(data)
      verifier.verify(signature)
    }
  }
}

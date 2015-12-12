package edu.ufl.dos15.crypto

import javax.crypto.KeyGenerator
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec
import java.security.SecureRandom
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.security.KeyPairGenerator
import java.security.Signature

object Crypto {
  private val random = new SecureRandom();

  def generateNonce(size: Int) = {
    val nonce = new Array[Byte](size)
    random.nextBytes(nonce).toString()
  }

  def generateToken(size: Int) = {
    val bytes = new Array[Byte](size)
    random.nextBytes(bytes)
    bytes.map("%02x".format(_)).mkString
  }

  object AES {
    def generateKey(keySize: Int): SecretKey = {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(keySize)
        keyGen.generateKey()
    }

    def generateIv(keySize: Int): IvParameterSpec = {
        val iv = new Array[Byte](keySize)
        random.nextBytes(iv)
        new IvParameterSpec(iv)
    }

    def encrypt(str: String, secKey: SecretKey, iv: IvParameterSpec): Array[Byte] = {
        encrypt(str.getBytes, secKey, iv)
    }

    def encrypt(data: Array[Byte], secKey: SecretKey, iv: IvParameterSpec): Array[Byte] = {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secKey, iv)
        cipher.doFinal(data)
    }

    def decrypt(data: Array[Byte], secKey: SecretKey, iv: IvParameterSpec): Array[Byte] = {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secKey, iv)
        cipher.doFinal(data)
    }
  }

  object RSA {
    def generateKeyPair(keySize: Int): KeyPair = {
      val keyGen = KeyPairGenerator.getInstance("RSA");
      keyGen.initialize(keySize);
      keyGen.generateKeyPair()
    }

    def encrypt(data: String, pubKey: PublicKey): Array[Byte] = {
      encrypt(data.getBytes, pubKey)
    }

    def encrypt(data: Array[Byte], pubKey: PublicKey): Array[Byte] = {
      val cipher = Cipher.getInstance("RSA")
      cipher.init(Cipher.ENCRYPT_MODE, pubKey)
      cipher.doFinal(data)
    }

    def decrypt(data: Array[Byte], priKey: PrivateKey): Array[Byte] = {
      val cipher = Cipher.getInstance("RSA")
      cipher.init(Cipher.DECRYPT_MODE, priKey)
      cipher.doFinal(data)
    }

    def sign(data: String, priKey: PrivateKey): Array[Byte] = {
      sign(data.getBytes, priKey)
    }

    def sign(data: Array[Byte], priKey: PrivateKey): Array[Byte] = {
      val signer = Signature.getInstance("SHA256withRSA")
      signer.initSign(priKey)
      signer.update(data)
      signer.sign
    }

    def verify(data: Array[Byte], signature: Array[Byte], pubKey: PublicKey): Boolean = {
      val verifier = Signature.getInstance("SHA256withRSA")
      verifier.initVerify(pubKey)
      verifier.update(data)
      verifier.verify(signature)
    }
  }
}

package edu.ufl.dos15.crypto

import javax.crypto.KeyGenerator
import javax.crypto.Cipher
import javax.crypto.SecretKey
import java.security.SecureRandom

object Crypto {
    val random = new SecureRandom();

    object AES {

        val numBits = 128

        def generateKey() = {
            val keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(numBits);
            keyGen.generateKey()
        }

        def encodeKey(key: SecretKey) = key.getEncoded()

        def decodeKey(key: Array[Byte]) = new SecretKeySpec(key, "AES")

        def generateIv() = {
            val iv = Array[Byte](numBits)
            random.nextBytes(iv)
            iv
        }

        def encrypt(data: Array[Byte], secKey: SecretKey, iv: IvParameterSpec) = {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secKey, iv)
            cipher.doFinal(data)
        }

        def decrypt(data: Array[Byte], secKey: SecretKey, iv: IvParameterSpec) = {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secKey, iv)
            cipher.doFinal(data)
        }
    }

    object RSA {

    }

}

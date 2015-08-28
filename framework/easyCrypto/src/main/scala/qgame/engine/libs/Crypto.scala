package qgame.engine.libs

import java.io._
import java.security._
import java.security.spec.{ PKCS8EncodedKeySpec, X509EncodedKeySpec }
import java.util.Base64
import javax.crypto._
import javax.crypto.spec.SecretKeySpec

import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.DigestUtils

import scala.Function.const
import scala.collection.immutable.Stream
import scala.collection.mutable.ArrayBuffer

/**
 * Created by kerr.
 */
object Crypto {
  def md5(text: String): String = {
    DigestUtils.md5Hex(text)
  }

  def md5(text: String, charset: String): String = {
    md5(text.getBytes(charset))
  }

  def md5(bytes: Array[Byte]): String = {
    DigestUtils.md5Hex(bytes)
  }

  def sha1(text: String): String = {
    DigestUtils.sha1Hex(text)
  }

  def sha1(text: String, charset: String): String = {
    sha1(text.getBytes(charset))
  }

  def sha1(bytes: Array[Byte]): String = {
    DigestUtils.sha1Hex(bytes)
  }

  def base64(bytes: Array[Byte]): String = {
    Base64.getEncoder.encodeToString(bytes)
  }

  def base64(base64: String): Array[Byte] = {
    Base64.getDecoder.decode(base64)
  }

  def hex2Bytes(hex: String): Array[Byte] = {
    Hex.decodeHex(hex.toCharArray)
  }

  def bytes2Hex(bytes: Array[Byte]): String = {
    Hex.encodeHexString(bytes)
  }

  /**
   * java实现RSA加密流程分析：
   * 1、甲方构建密钥对儿，将公钥公布给乙方，将私钥保留。
   * 2、甲方使用私钥加密数据，然后用私钥对加密后的数据签名，发送给乙方签名以及加密后的数据；乙方使用公钥、签名来验证待解密数据是否有效，如果有效使用公钥对数据解密。
   * 3、乙方使用公钥加密数据，向甲方发送经过加密后的数据；甲方获得加密数据，通过私钥解密。
   */

  object RSA {
    def decodePrivateKey(encodedKey: Array[Byte]): PrivateKey = {
      val spec = new PKCS8EncodedKeySpec(encodedKey)
      val factory = KeyFactory.getInstance("RSA")
      factory.generatePrivate(spec)
    }

    def decodePublicKey(encodedKey: Array[Byte]): PublicKey = {
      val spec = new X509EncodedKeySpec(encodedKey)
      val factory = KeyFactory.getInstance("RSA")
      factory.generatePublic(spec)
    }

    def encrypt(key: PublicKey, data: Array[Byte]): Array[Byte] = {
      val cipher = Cipher.getInstance("RSA")
      cipher.init(Cipher.ENCRYPT_MODE, key)
      cipher.doFinal(data)
    }

    def encrypt(key: PrivateKey, data: Array[Byte]): Array[Byte] = {
      val cipher = Cipher.getInstance("RSA")
      cipher.init(Cipher.ENCRYPT_MODE, key)
      cipher.doFinal(data)
    }

    def decrypt(key: PublicKey, data: Array[Byte]): Array[Byte] = {
      val cipher = Cipher.getInstance("RSA")
      cipher.init(Cipher.DECRYPT_MODE, key)
      cipher.doFinal(data)
    }

    def decryptLong(key: PublicKey, data: Array[Byte], length: Int): Array[Byte] = {
      val cipher = Cipher.getInstance("RSA")
      cipher.init(Cipher.DECRYPT_MODE, key)
      val arrayBuffer = new ArrayBuffer[Byte](data.length)
      data.grouped(length).zipWithIndex.foreach {
        case (buffer, index) =>
          val offset = length * index
          val size = buffer.length
          arrayBuffer ++= cipher.doFinal(data, offset, size)
      }
      arrayBuffer.toArray
    }

    def decrypt(key: PrivateKey, data: Array[Byte]): Array[Byte] = {
      val cipher = Cipher.getInstance("RSA")
      cipher.init(Cipher.DECRYPT_MODE, key)
      cipher.doFinal(data)
    }

    def sign(key: PrivateKey, data: Array[Byte]): Array[Byte] = {
      val signer = Signature.getInstance("SHA1withRSA")
      signer.initSign(key)
      signer.update(data)
      signer.sign
    }

    def verify(key: PublicKey, signature: Array[Byte], data: Array[Byte]): Boolean = {
      val verifier = Signature.getInstance("SHA1withRSA")
      verifier.initVerify(key)
      verifier.update(data)
      verifier.verify(signature)
    }
  }

  object AES {
    def encrypt(key: SecretKey, data: Array[Byte]): Array[Byte] = {
      val cipher = Cipher.getInstance("AES")
      cipher.init(Cipher.ENCRYPT_MODE, key)
      cipher.doFinal(data)
    }

    def decrypt(key: SecretKey, data: Array[Byte]): Array[Byte] = {
      val cipher = Cipher.getInstance("AES")
      cipher.init(Cipher.DECRYPT_MODE, key)
      cipher.doFinal(data)
    }

    def generateSecretKey: SecretKey = {
      val generator = KeyGenerator.getInstance("AES")
      generator.init(128)
      generator.generateKey
    }

    def decodeSecretKey(encodedKey: Array[Byte]): SecretKey =
      new SecretKeySpec(encodedKey, "AES")
  }

  object IO {
    def readPrivateKey(filePath: String): PrivateKey =
      RSA.decodePrivateKey(readEncodedRSAKey(filePath))

    def readPublicKey(filePath: String): PublicKey =
      RSA.decodePublicKey(readEncodedRSAKey(filePath))

    def readEncodedRSAKey(filePath: String): Array[Byte] = {
      withDataInputStream(filePath) { stream =>
        val nameLength = stream.readInt
        stream.skip(nameLength)

        val keyLength = stream.readInt
        val key: Array[Byte] = Array.ofDim(keyLength)
        stream.read(key)

        key
      }
    }

    def readSecureFile(filePath: String): (Array[Byte], Array[Byte], Array[Byte]) = {
      withDataInputStream(filePath) { stream =>
        val keyLength = stream.readInt
        val key: Array[Byte] = Array.ofDim(keyLength)
        stream.read(key)

        val signatureLength = stream.readInt
        val signature: Array[Byte] = Array.ofDim(signatureLength)
        stream.read(signature)

        val data = readStream(stream)

        (key, signature, data)
      }
    }

    def writeSecureFile(encryptedSecretKey: Array[Byte], signature: Array[Byte], encryptedData: Array[Byte], filePath: String): Unit = {
      withDataOutputStream(filePath) { stream =>
        stream.writeInt(encryptedSecretKey.length)
        stream.write(encryptedSecretKey)

        stream.writeInt(signature.length)
        stream.write(signature)

        stream.write(encryptedData)
      }
    }

    def readStream(stream: InputStream): Array[Byte] =
      Stream.continually(stream.read()).takeWhile(-1 != _).map(_.toByte).toArray

    def readFile(filePath: String): Array[Byte] =
      withDataInputStream(filePath)(readStream)

    def writeFile(filePath: String, data: Array[Byte]): Unit =
      withDataOutputStream(filePath)(_.write(data))

    def withCloseable[A <: Closeable, B](closeable: A)(f: A => B): B =
      const(f(closeable))(closeable.close())

    def withDataInputStream[A](filePath: String): (DataInputStream => A) => A =
      withCloseable(new DataInputStream(new FileInputStream(filePath)))(_)

    def withDataOutputStream[A](filePath: String): (DataOutputStream => A) => A =
      withCloseable(new DataOutputStream(new FileOutputStream(filePath)))(_)
  }
}

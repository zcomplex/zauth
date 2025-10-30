package xauth.util

import java.io.{File, FileInputStream}
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.security.*
import java.time.ZoneOffset.UTC
import java.time.{Instant, LocalDateTime, LocalTime}
import java.util.Base64.getEncoder
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import scala.util.Random

package object ext:

  /**
    * Converts a string to a new string that represents
    * `MD5` hash of this string.
    */
  extension (s: String)
    def md5: String =
      MessageDigest
        .getInstance("MD5")
        .digest(s.getBytes)
        .map(0xFF & _)
        .map("%02x".format(_))
        .foldLeft("")(_ + _)

  extension (s: String)
    def base64: String = getEncoder.encodeToString(s.getBytes)

  /**
   * Generates a random string.
   * @param cs A sequence of character used to make string.
   */
  private class RandomString(cs: Seq[Char], random: Random = new SecureRandom):
    def random(length: Int): String =
      val sb = new StringBuilder
      for (i <- 1 to length) do
        val randomNum = random.nextInt(cs.length)
        sb.append(cs(randomNum))
      sb.toString

  /** Generates a random string. */
  extension (cs: Seq[Char])
    infix def random(length: Int): String = new RandomString(cs).random(length)

  /** Retrieves the epoch milliseconds. */
  extension (t: LocalTime)
    def toEpochMilli: Long = LocalDateTime
      .ofEpochSecond(0, 0, UTC)
      .plusHours(t.getHour)
      .plusMinutes(t.getMinute)
      .plusSeconds(t.getSecond)
      .toInstant(UTC)
      .toEpochMilli

  /** Retrieves the local time. */
  extension (l: Long)
    def toEpochTime: LocalTime =
      LocalDateTime.ofInstant(Instant.ofEpochMilli(l), UTC).toLocalTime

  extension (file: File)
    def bytes: Array[Byte] =
      val fis = new FileInputStream(file)
      try LazyList.continually(fis.read).takeWhile(_ != -1).map(_.toByte).toArray
      finally fis.close()

  extension (bytes: Array[Byte])

    /** Read bytes as private key. */
    def toPrivateKey: PrivateKey =
      val spec: PKCS8EncodedKeySpec = new PKCS8EncodedKeySpec(bytes)
      val kf: KeyFactory = KeyFactory.getInstance("RSA")
      kf.generatePrivate(spec)

    /** Read bytes as public key. */
    def toPublicKey: PublicKey =
      val spec: X509EncodedKeySpec = new X509EncodedKeySpec(bytes)
      val kf: KeyFactory = KeyFactory.getInstance("RSA")
      kf.generatePublic(spec)

    def toSecretKey: SecretKey = new SecretKeySpec(bytes, 0, bytes.length, "RSA")
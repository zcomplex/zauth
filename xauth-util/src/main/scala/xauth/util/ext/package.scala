package xauth.util

import java.security.{MessageDigest, SecureRandom}
import java.time.ZoneOffset.UTC
import java.time.{Instant, LocalDateTime, LocalTime}
import java.util.Base64.getEncoder
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
package xauth.util.time

import java.time.LocalDateTime

case class LocalDateTimeInterval(start: LocalDateTime, end: LocalDateTime):
  override def toString: String = s"$start/$end"

object LocalDateTimeInterval:
  def unapply(s: String): LocalDateTimeInterval =
    val ss = s.split("/")
    LocalDateTimeInterval(
      LocalDateTime.parse(ss(0)),
      LocalDateTime.parse(ss(1))
    )

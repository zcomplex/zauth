package xauth.util.time

import java.time.LocalTime

case class LocalTimeInterval(start: LocalTime, end: LocalTime):
  override def toString: String = s"$start/$end"

object LocalTimeInterval:
  def unapply(s: String): LocalTimeInterval =
    val ss = s.split("/")
    LocalTimeInterval(
      LocalTime.parse(ss(0)),
      LocalTime.parse(ss(1))
    )
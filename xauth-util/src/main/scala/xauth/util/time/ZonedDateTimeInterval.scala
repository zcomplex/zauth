package xauth.util.time

import java.time.{LocalDateTime, ZoneId}

case class ZonedDateTimeInterval(start: ZonedDate, end: ZonedDate):
  override def toString: String = s"$start/$end"

  def atZone(zoneId: ZoneId): ZonedDateTimeInterval =
    ZonedDateTimeInterval(start.atZone(zoneId), end.atZone(zoneId))

object ZonedDateTimeInterval:
  def unapply(s: String): ZonedDateTimeInterval =
    val ss = s.split("/")
    ZonedDateTimeInterval(
      ZonedDate.from(
        LocalDateTime.parse(ss(0))
      ),
      ZonedDate.from(
        LocalDateTime.parse(ss(1))
      )
    )

  val Eternity: ZonedDateTimeInterval =
    ZonedDateTimeInterval(ZonedDate.Epoch, ZonedDate.Max)
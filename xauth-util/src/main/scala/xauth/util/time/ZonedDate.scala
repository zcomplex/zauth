package xauth.util.time

import java.time.*
import java.time.ZoneOffset.UTC

case class ZonedDate(date: ZonedDateTime, zoneId: ZoneId):
  def toOffsetDateTime: OffsetDateTime =
    date.toOffsetDateTime

  def atZone(zoneId: ZoneId): ZonedDate =
    ZonedDate.from(date.toLocalDateTime, zoneId)

object ZonedDate:

  val Epoch: ZonedDate =
    ZonedDate(
      OffsetDateTime
        .ofInstant(Instant.EPOCH, UTC)
        .toZonedDateTime,
      UTC
    )

  val Max: ZonedDate =
    ZonedDate(
      OffsetDateTime
        .MAX
        .atZoneSimilarLocal(UTC),
      UTC
    )

  def now: ZonedDate = now()

  def from(date: LocalDateTime, zoneId: ZoneId = UTC): ZonedDate =
    ZonedDate(date.atZone(zoneId), zoneId)

  infix def now(zoneId: ZoneId = UTC): ZonedDate =
    ZonedDate(ZonedDateTime.now(zoneId), zoneId)

  infix def from(date: LocalDateTime): ZonedDate =
    from(date, UTC)

  infix def from(date: OffsetDateTime): ZonedDate =
    from(date.toLocalDateTime)
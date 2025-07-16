package xauth.infrastructure.mongo

import reactivemongo.api.bson.{BSONDateTime, BSONDocument, BSONHandler, BSONString, BSONValue}
import xauth.util.DataFormat.iso8601DateFormat
import xauth.util.Uuid
import xauth.util.ext.{toEpochMilli, toEpochTime}
import xauth.util.time.{LocalDateTimeInterval, LocalTimeInterval, ZonedDate, ZonedDateTimeInterval}

import java.text.SimpleDateFormat
import java.time.*
import java.time.ZoneOffset.UTC
import java.util.{Date, Locale}
import scala.util.{Success, Try}

package object bson:

  object handler:

    import bson.ext.*

    given uuidBsonHandler: BSONHandler[Uuid] = new BSONHandler[Uuid]:
      override def readTry(b: BSONValue): Try[Uuid] = b.asTry[BSONString].map(b => Uuid(b.value))
      override def writeTry(u: Uuid): Try[BSONValue] = Success(BSONString(u.stringValue))

    given dateBsonHandler: BSONHandler[Date] = new BSONHandler[Date]:
      override def readTry(b: BSONValue): Try[Date] =
        b.asTry[BSONString].map(s => new SimpleDateFormat(iso8601DateFormat).parse(s.value))

      override def writeTry(d: Date): Try[BSONValue] =
        Success(BSONString(new SimpleDateFormat(iso8601DateFormat).format(d)))

    given zonedDateBsonHandler: BSONHandler[ZonedDate] = new BSONHandler[ZonedDate]:
      override def readTry(b: BSONValue): Try[ZonedDate] = b.asTry[BSONDocument].map(_.toZonedDate)
      override def writeTry(d: ZonedDate): Try[BSONDocument] = Success(d.toBson)

    given zonedDateTimeIntervalBsonHandler: BSONHandler[ZonedDateTimeInterval] = new BSONHandler[ZonedDateTimeInterval]:
      override def readTry(b: BSONValue): Try[ZonedDateTimeInterval] =
        b.asTry[BSONDocument].map(_.toZonedDate)
        for {
          d <- b.asTry[BSONDocument]
          s = d.get("start").map(_.asInstanceOf[BSONDocument].toZonedDate) getOrElse ZonedDate.Epoch
          e = d.get("end").map(_.asInstanceOf[BSONDocument].toZonedDate) getOrElse ZonedDate.Epoch
        } yield ZonedDateTimeInterval(s, e)

      override def writeTry(d: ZonedDateTimeInterval): Try[BSONDocument] = Success:
        BSONDocument("start" -> d.start.toBson, "end" -> d.end.toBson)

    given zoneIdBsonHandler: BSONHandler[ZoneId] = new BSONHandler[ZoneId]:
      override def readTry(b: BSONValue): Try[ZoneId] = b.asTry[BSONString].map(s => ZoneId.of(s.value))
      override def writeTry(z: ZoneId): Try[BSONValue] = Success(BSONString(z.getId))

    given localTimeIntervalBsonHandler: BSONHandler[LocalTimeInterval] = new BSONHandler[LocalTimeInterval]:
      override def readTry(b: BSONValue): Try[LocalTimeInterval] =
        for
          d <- b.asTry[BSONDocument]
          s = d.get("start")
            .map(_.asInstanceOf[BSONDateTime])
            .map(_.value)
            .map(_.toEpochTime) getOrElse LocalTime.MIN
          e = d.get("end")
            .map(_.asInstanceOf[BSONDateTime])
            .map(_.value)
            .map(_.toEpochTime) getOrElse LocalTime.MAX
        yield LocalTimeInterval(s, e)

      override def writeTry(d: LocalTimeInterval): Try[BSONValue] =
        Success:
          BSONDocument(
            "start" -> BSONDateTime(d.start.toEpochMilli),
            "end" -> BSONDateTime(d.end.toEpochMilli)
          )

    given localDateTimeIntervalBsonHandler: BSONHandler[LocalDateTimeInterval] = new BSONHandler[LocalDateTimeInterval]:
      override def readTry(b: BSONValue): Try[LocalDateTimeInterval] =
        b.asTry[BSONString] map (_.value.split("/")) map :
          case Array(a, b) => LocalDateTimeInterval(LocalDateTime.parse(a), LocalDateTime.parse(b))

      override def writeTry(d: LocalDateTimeInterval): Try[BSONValue] =
        Success(BSONString(d.toString))

    given localeBsonHandler: BSONHandler[Locale] = new BSONHandler[Locale]:
      override def readTry(b: BSONValue): Try[Locale] =
        b.asTry[BSONString] map : s =>
          val i = s.value.lastIndexOf('-')
          new Locale(s.value.substring(0, i), s.value.substring(i + 1))

      override def writeTry(l: Locale): Try[BSONValue] =
        Success(BSONString(s"${l.getLanguage}-${l.getCountry}"))

  object ext:

    extension (b: BSONDocument)
      def toZonedDate: ZonedDate =
        val t = b.get("date")
          .map(_.asInstanceOf[BSONDateTime])
          .map(_.value)
          .map(Instant.ofEpochMilli) getOrElse Instant.EPOCH
        val z = b.get("zone")
          .map(_.asInstanceOf[BSONString])
          .map(_.value)
          .map(ZoneId.of) getOrElse UTC
        ZonedDate(ZonedDateTime.ofInstant(t, z), z)

    // Useful for handwritten queries
    extension (d: ZonedDate)
      def toBson: BSONDocument =
        val utcMs = d
          .date
          .toLocalDateTime
          .atZone(d.zoneId)
          .toInstant
          .toEpochMilli
        BSONDocument("date" -> BSONDateTime(utcMs), "zone" -> BSONString(d.zoneId.getId))

    extension (i: ZonedDateTimeInterval)
      def toBson: BSONDocument =
        BSONDocument("start" -> i.start.toBson, "end" -> i.end.toBson)
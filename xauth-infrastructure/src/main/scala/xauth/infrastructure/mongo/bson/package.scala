package xauth.infrastructure.mongo

import io.circe.{Json, JsonObject}
import reactivemongo.api.bson.{BSONArray, BSONBoolean, BSONDateTime, BSONDocument, BSONDouble, BSONHandler, BSONInteger, BSONLong, BSONNull, BSONString, BSONValue}
import xauth.core.common.model.{AuthRole, AuthStatus, ContactType, Permission}
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

    // xauth.core.common.model

    given contactTypeBsonHandler: BSONHandler[ContactType] = new BSONHandler[ContactType]:
      override def readTry(b: BSONValue): Try[ContactType] = b.asTry[BSONString] map { s => ContactType.fromValue(s.value) }
      override def writeTry(s: ContactType): Try[BSONValue] = Success(BSONString(s.value))
  
    given authRoleBsonHandler: BSONHandler[AuthRole] = new BSONHandler[AuthRole]:
      override def readTry(b: BSONValue): Try[AuthRole] = b.asTry[BSONString] map { s => AuthRole.fromValue(s.value) }
      override def writeTry(s: AuthRole): Try[BSONValue] = Success(BSONString(s.value))
  
    given permissionBsonHandler: BSONHandler[Permission] = new BSONHandler[Permission]:
      override def readTry(b: BSONValue): Try[Permission] = b.asTry[BSONString] map { s => Permission.fromValue(s.value) }
      override def writeTry(s: Permission): Try[BSONValue] = Success(BSONString(s.value))
  
    given authStatusBsonHandler: BSONHandler[AuthStatus] = new BSONHandler[AuthStatus]:
      override def readTry(b: BSONValue): Try[AuthStatus] = b.asTry[BSONString] map { s => AuthStatus.fromValue(s.value) }
      override def writeTry(s: AuthStatus): Try[BSONValue] = Success(BSONString(s.value))

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

    extension (j: Json)
      def toBson: BSONValue = j.fold(
        jsonNull = BSONNull,
        jsonBoolean = b => BSONBoolean(b),
        jsonNumber = n => n
          .toBigDecimal
          .flatMap(d =>
            if d.isValidInt then Some(BSONInteger(d.intValue))
            else if d.isValidLong then Some(BSONLong(d.longValue))
            else Some(BSONDouble(d.doubleValue))
          )
          .getOrElse(BSONDouble(n.toDouble)),
        jsonString = str => BSONString(str),
        jsonArray = arr => BSONArray(arr.map(_.toBson)),
        jsonObject = obj => BSONDocument(obj.toMap.view.mapValues(_.toBson).toMap)
      )

    extension (b: BSONValue)
      def toJson: Json = b match
        case BSONNull        => Json.Null
        case BSONBoolean(b)  => Json.fromBoolean(b)
        case BSONInteger(i)  => Json.fromInt(i)
        case BSONLong(l)     => Json.fromLong(l)
        case BSONDouble(d)   => Json.fromDoubleOrNull(d)
        case BSONString(s)   => Json.fromString(s)
        case BSONArray(a)    => Json.fromValues(a.map(_.toJson))
        case d: BSONDocument =>
          Json.fromJsonObject:
            JsonObject.fromMap:
              d.elements.map { e => e.name -> e.value.toJson }.toMap
        case o => throw new RuntimeException(s"unsupported bson: $o")
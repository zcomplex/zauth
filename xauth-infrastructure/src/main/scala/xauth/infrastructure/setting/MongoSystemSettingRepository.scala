package xauth.infrastructure.setting

import reactivemongo.api.Cursor
import reactivemongo.api.bson.{BSONDocument, BSONDocumentHandler, BSONString}
import xauth.core.domain.system.model.{SettingKey, SystemSetting}
import xauth.core.domain.system.port.SystemSettingRepository
import xauth.infrastructure.mongo.DefaultMongoClient
import xauth.infrastructure.mongo.SystemCollection.Setting as SettingC
import xauth.infrastructure.setting.MongoSystemSettingRepository
import zio.{Tag, Task, ZIO, ZLayer}

import scala.util.{Failure, Success, Try}

private class MongoSystemSettingRepository(mongo: DefaultMongoClient) extends SystemSettingRepository:

  private given systemSettingBsonHandler: BSONDocumentHandler[SystemSetting] = new BSONDocumentHandler[SystemSetting]:
    override def readDocument(b: BSONDocument): Try[SystemSetting] =
      b.asTry[BSONDocument] flatMap: d =>
        val setting = for
          k <- d
            .get("_id")
            .collect:
              case s: BSONString => SettingKey.fromValue(s.value)
          v <- d
            .get("value")
            .collect:
              case s: BSONString => s.value
        yield SystemSetting(k, v)

        setting match
          case Some(s) => Success(s)
          case None => Failure(new NoSuchFieldException("unable to parse key/value bson document"))

    override def writeTry(s: SystemSetting): Try[BSONDocument] =
      Success:
        BSONDocument("_id" -> s.key.value, "value" -> s.value)

  /** Saves the setting for the given key and value. */
  override def save[A](k: SettingKey, v: A): Task[A] =
    save(k.settingWith(v.toString)) map { _ => v }

  /** Deletes setting by its key. */
  override def delete(k: SettingKey): Task[Boolean] =
    mongo.collection(SettingC) flatMap:
      c => ZIO.fromFuture:
        implicit ec => c
          .delete.one:
            BSONDocument("_id" -> k.value)
          .map(_.n > 0)
  
  /** Finds all settings. */
  override def findAll: Task[Seq[SystemSetting]] =
    mongo.collection(SettingC) flatMap:
      c => ZIO.fromFuture:
        implicit ec => c
          .find(BSONDocument.empty)
          .cursor[SystemSetting]()
          .collect[List](-1, Cursor.FailOnError[List[SystemSetting]]())

  /** Finds setting by its key. */
  override def find(k: SettingKey): Task[Option[SystemSetting]] =
    mongo.collection(SettingC) flatMap:
      c => ZIO.fromFuture:
        implicit ec => c
          .find:
            BSONDocument("_id" -> k.value)
          .one[SystemSetting]

  /** Saves setting on persistence system. */
  override def save(s: SystemSetting): Task[SystemSetting] =
    mongo.collection(SettingC) flatMap:
      c => ZIO.fromFuture:
        implicit ec => c.insert.one(s) map { _ => s }

object MongoSystemSettingRepository:

  val layer: ZLayer[DefaultMongoClient, Nothing, SystemSettingRepository] =
    ZLayer.fromZIO:
      ZIO.service[DefaultMongoClient] map:
        mongo => new MongoSystemSettingRepository(mongo)
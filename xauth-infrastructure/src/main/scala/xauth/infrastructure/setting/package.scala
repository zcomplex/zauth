package xauth.infrastructure

import reactivemongo.api.bson.{BSONDocument, BSONDocumentHandler, BSONString}
import xauth.core.domain.system.model.{SettingKey, SystemSetting}

import scala.util.{Failure, Success, Try}

package object setting:
  
  object bson:
    object handler:
      
      given systemSettingBsonHandler: BSONDocumentHandler[SystemSetting] = new BSONDocumentHandler[SystemSetting]:
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
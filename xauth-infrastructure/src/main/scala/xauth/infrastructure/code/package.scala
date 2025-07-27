package xauth.infrastructure

import reactivemongo.api.bson.{BSONDocumentHandler, BSONHandler, BSONString, BSONValue, Macros}
import xauth.core.domain.code.model.{AccountCodeType, CodeData, UserContactData}

import scala.util.{Success, Try}

package object code:

  object bson:

    import xauth.infrastructure.mongo.bson.handler.given
    import xauth.infrastructure.user.bson.handler.given

    given accountCodeTypeBsonHandler: BSONHandler[AccountCodeType] = new BSONHandler[AccountCodeType]:
      override def readTry(b: BSONValue): Try[AccountCodeType] = b.asTry[BSONString] map { s => AccountCodeType.fromValue(s.value) }
      override def writeTry(s: AccountCodeType): Try[BSONValue] = Success(BSONString(s.value))

    given userContactDataBsonHandler: BSONDocumentHandler[UserContactData] = Macros.handler[UserContactData]

    given codeDataBsonHandler: BSONDocumentHandler[CodeData] = Macros.handler[CodeData]

    given accountCodeBsonHandler: BSONDocumentHandler[AccountCodeDo] = Macros.handler[AccountCodeDo]
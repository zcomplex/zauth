package xauth.infrastructure

import io.circe.Encoder
import reactivemongo.api.bson.{BSONDocumentHandler, BSONValue, Macros}
import xauth.core.domain.user.model.{AppInfo, UserContact, UserInfo}

import scala.util.{Success, Try}

package object user:

  object bson:
    object handler:
      
      import xauth.infrastructure.mongo.bson.handler.given
  
      given appInfoBsonHandler: BSONDocumentHandler[AppInfo] = Macros.handler[AppInfo]
      
      given userContactBsonHandler: BSONDocumentHandler[UserContact] = Macros.handler[UserContact]
      given userInfoBsonHandler: BSONDocumentHandler[UserInfo] = Macros.handler[UserInfo]
      
      given userBsonHandler: BSONDocumentHandler[UserDo] = Macros.handler[UserDo]
  
  object json:
    
    given appInfo: Encoder[AppInfo] = ???
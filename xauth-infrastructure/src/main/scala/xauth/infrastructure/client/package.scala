package xauth.infrastructure

import reactivemongo.api.bson.{BSONDocumentHandler, Macros}

package object client:

  object bson:
    object handler:
      
      import xauth.infrastructure.mongo.bson.handler.uuidBsonHandler

      given clientBsonHandler: BSONDocumentHandler[ClientDo] = Macros.handler[ClientDo]
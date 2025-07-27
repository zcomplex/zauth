package xauth.infrastructure

import reactivemongo.api.bson.{BSONDocumentHandler, Macros}

package object client:

  object bson:
    object handler:

      given clientBsonHandler: BSONDocumentHandler[ClientDo] = Macros.handler[ClientDo]
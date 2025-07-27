package xauth.infrastructure

import reactivemongo.api.bson.{BSONDocumentHandler, Macros}

package object tenant:

  object bson:
    object handler:

      import xauth.infrastructure.mongo.bson.handler.uuidBsonHandler

      given tenantBsonHandler: BSONDocumentHandler[TenantDo] = Macros.handler[TenantDo]
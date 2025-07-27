package xauth.infrastructure.client

import reactivemongo.api.bson.Macros.Annotations.Key
import xauth.core.domain.client.model.Client
import xauth.util.Uuid

import java.time.Instant

case class ClientDo
(
  @Key("_id")
  id: String,
  secret: String,
  createdAt: Instant,
  createdBy: Uuid,
  updatedAt: Instant,
  updatedBy: Uuid
)

object ClientDo:
  
  extension (c: Client)
    def fromDomain: ClientDo =
      ClientDo(
        id = c.id,
        secret = c.secret,
        createdAt = c.createdAt,
        createdBy = c.createdBy,
        updatedAt = c.updatedAt,
        updatedBy = c.updatedBy
      )

  extension (c: ClientDo)
    def toDomain: Client =
      Client(
        id = c.id,
        secret = c.secret,
        createdAt = c.createdAt,
        createdBy = c.createdBy,
        updatedAt = c.updatedAt,
        updatedBy = c.updatedBy
      )
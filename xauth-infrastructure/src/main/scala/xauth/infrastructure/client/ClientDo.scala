package xauth.infrastructure.client

import reactivemongo.api.bson.Macros.Annotations.Key
import xauth.core.domain.client.model.Client

import java.time.Instant

case class ClientDo
(
  @Key("_id")
  id: String,
  secret: String,
  registeredAt: Instant,
  updatedAt: Instant
)

object ClientDo:
  
  extension (c: Client)
    def fromDomain: ClientDo =
      ClientDo(
        id = c.id,
        secret = c.secret,
        registeredAt = c.registeredAt,
        updatedAt = c.updatedAt
      )

  extension (c: ClientDo)
    def toDomain: Client =
      Client(
        id = c.id,
        secret = c.secret,
        registeredAt = c.registeredAt,
        updatedAt = c.updatedAt
      )
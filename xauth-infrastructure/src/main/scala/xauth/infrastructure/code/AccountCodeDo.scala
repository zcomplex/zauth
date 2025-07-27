package xauth.infrastructure.code

import reactivemongo.api.bson.Macros.Annotations.Key
import xauth.core.domain.code.model.{AccountCode, AccountCodeType, CodeData}
import xauth.util.Uuid

import java.time.Instant

case class AccountCodeDo
(
  @Key("_id")
  code: String,
  kind: AccountCodeType,
  refId: Uuid,
  data: Option[CodeData],
  expiresAt: Instant,
  createdBy: Uuid,
  createdAt: Instant,
  updatedBy: Uuid,
  updatedAt: Instant
)

object AccountCodeDo:

  extension (c: AccountCode)
    def fromDomain: AccountCodeDo =
      AccountCodeDo(
        code = c.code,
        kind = c.kind,
        refId = c.refId,
        data = c.data,
        expiresAt = c.expiresAt,
        createdBy = c.createdBy,
        createdAt = c.createdAt,
        updatedBy = c.updatedBy,
        updatedAt = c.updatedAt
      )

  extension (c: AccountCodeDo)
    def toDomain: AccountCode =
      AccountCode(
        code = c.code,
        kind = c.kind,
        refId = c.refId,
        data = c.data,
        expiresAt = c.expiresAt,
        createdBy = c.createdBy,
        createdAt = c.createdAt,
        updatedBy = c.updatedBy,
        updatedAt = c.updatedAt
      )
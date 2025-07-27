package xauth.core.domain.code.model

import xauth.core.domain.user.model.UserContact
import xauth.util.Uuid

import java.time.Instant

sealed trait CodeData
final case class UserContactData(contact: UserContact) extends CodeData

case class AccountCode
(
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
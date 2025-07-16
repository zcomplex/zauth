package xauth.core.domain.user.model

import xauth.core.common.model.{AuthRole, AuthStatus, ContactType, Permission}
import xauth.util.Uuid
import xauth.util.time.ZonedDate

/** Defines user information. */
case class UserInfo
(
  firstName: String,
  lastName: String,
  company: String,
  contacts: Seq[UserContact]
)

/** Defines minimum information of a contact */
case class UserContact
(
  `type`: ContactType,
  value: String,
  description: Option[String],
  trusted: Boolean
)

/** Defines application information */
case class AppInfo
(
  name: String,
  permissions: Set[Permission]
)

case class User
(
  id: Uuid,
  username: String,
  password: String,
  salt: String,
  parentId: Option[Uuid],
  roles: Seq[AuthRole],
  applications: Seq[AppInfo] = Nil,
  status: AuthStatus,
  description: Option[String],
  info: UserInfo,
  createdBy: Uuid,
  createdAt: ZonedDate,
  updatedBy: Uuid,
  updatedAt: ZonedDate
)
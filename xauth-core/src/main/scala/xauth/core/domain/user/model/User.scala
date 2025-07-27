package xauth.core.domain.user.model

import xauth.core.common.model.ContactType.*
import xauth.core.common.model.{AuthRole, AuthStatus, ContactType, Permission}
import xauth.util.Uuid

import java.time.Instant

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
  kind: ContactType,
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
  createdAt: Instant,
  updatedBy: Uuid,
  updatedAt: Instant
):

  def contact(t: ContactType, trusted: Boolean = true): Option[String] =
    info.contacts
      .find(c => c.kind == t && c.trusted == trusted)
      .map(_.value)

  def email(trusted: Boolean = true): Option[String] = 
    contact(Email, trusted)

  def mobileNumber(trusted: Boolean = true): Option[String] =
    contact(MobileNumber, trusted)
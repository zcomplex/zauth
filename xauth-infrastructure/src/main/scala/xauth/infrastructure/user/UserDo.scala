package xauth.infrastructure.user

import reactivemongo.api.bson.Macros.Annotations.Key
import xauth.core.common.model.{AuthRole, AuthStatus}
import xauth.core.domain.user.model.{AppInfo, User, UserInfo}
import xauth.util.Uuid

import java.time.Instant

case class UserDo
(
  @Key("_id")
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
)

object UserDo:

  extension (u: User)
    def fromDomain: UserDo =
      UserDo(
        id = u.id,
        username = u.username,
        password = u.password,
        salt = u.salt,
        parentId = u.parentId,
        roles = u.roles,
        applications = u.applications,
        status = u.status,
        description = u.description,
        info = u.info,
        createdBy = u.createdBy,
        createdAt = u.createdAt,
        updatedBy = u.updatedBy,
        updatedAt = u.updatedAt
      )

  extension (u: UserDo)
    def toDomain: User =
      User(
        id = u.id,
        username = u.username,
        password = u.password,
        salt = u.salt,
        parentId = u.parentId,
        roles = u.roles,
        applications = u.applications,
        status = u.status,
        description = u.description,
        info = u.info,
        createdBy = u.createdBy,
        createdAt = u.createdAt,
        updatedBy = u.updatedBy,
        updatedAt = u.updatedAt
      )
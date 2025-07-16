package xauth.core.application.usecase

import xauth.core.common.model.{AuthRole, AuthStatus}
import xauth.core.domain.user.model.{AppInfo, User, UserInfo}
import xauth.core.domain.user.port.{UserRepository, UserService}
import xauth.core.domain.workspace.model.Workspace
import xauth.util.Uuid
import com.lambdaworks.crypto.SCryptUtil.*
import zio.{Task, URLayer, ZIO, ZLayer}
import xauth.util.ext.random
import xauth.util.time.ZonedDate

class UserServiceImpl(repository: UserRepository) extends UserService:

  /**
   * Creates new user for the given workspace.
   *
   * @param username Username.
   * @param password User password that it will be encrypted.
   * @param userInfo User information.
   * @return Returns a [[Task]] that boxes just created user.
   */
  override def create(username: String, password: String, description: Option[String], parentId: Option[Uuid], userInfo: UserInfo, status: AuthStatus, applications: List[AppInfo], roles: AuthRole*)(using w: Workspace): Task[User] =
    // specific workspace timezone
    val now = ZonedDate.now(w.configuration.zoneId)
    
    val encryption = cryptWithSalt(password)
    
    val user = User(
      id = Uuid(),
      username = username,
      password = encryption.hash,
      salt = encryption.salt,
      parentId = parentId,
      roles = roles.toList,
      status = status,
      description = description,
      applications = applications,
      info = userInfo,
      createdBy = Uuid.Zero, // todo: read it by context
      createdAt = now,
      updatedBy = Uuid.Zero, // todo: read it by context
      updatedAt = now
    )

    repository save user
    // todo: status !enabled: trust account >> async message dispatch

  private def cryptWithSalt(s: String): (salt: String, hash: String) =
    val salt = (('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9')).random(79)
    val pass = scrypt(salt + s, 16, 16, 16) // 79 output bytes
    (salt, pass)

  /**
   * Checks if hashed string has been encrypted for given salt and string.
   *
   * @param s  The salt.
   * @param ss The string to check with salt `s`.
   * @param hs The hashed string.
   * @return Returns `true` if `hs` has been encrypted by the given `s` salt and
   *         the string `s`, returns false otherwise.
   */
  def checkWithSalt(s: String, ss: String, hs: String): Boolean = check(s + ss, hs)

object UserServiceImpl:

  lazy val layer: URLayer[UserRepository, UserServiceImpl] =
    ZLayer.fromZIO:
      ZIO.service[UserRepository] map:
        new UserServiceImpl(_)
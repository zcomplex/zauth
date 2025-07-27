package xauth.core.application.usecase

import com.lambdaworks.crypto.SCryptUtil.*
import xauth.core.common.model.AuthStatus.Disabled
import xauth.core.common.model.{AuthRole, AuthStatus}
import xauth.core.domain.user.model.{AppInfo, User, UserInfo}
import xauth.core.domain.user.port.{UserRepository, UserService}
import xauth.core.domain.workspace.model.Workspace
import xauth.core.spi.{AccountEvent, AccountEventDispatcher}
import xauth.util.Uuid
import xauth.util.ext.random
import zio.{Task, URLayer, ZIO, ZLayer}

import java.time.Instant

class UserServiceImpl(repository: UserRepository, dispatcher: AccountEventDispatcher) extends UserService:

  /**
   * Creates new user for the given workspace.
   *
   * @param username Username.
   * @param password User password that it will be encrypted.
   * @param userInfo User information.
   * @return Returns a [[Task]] that boxes just created user.
   */
  override def create(username: String, password: String, description: Option[String], parentId: Option[Uuid], userInfo: UserInfo, status: AuthStatus, applications: List[AppInfo], roles: AuthRole*)(using w: Workspace): Task[User] =

    val now = Instant.now
    
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

    for
      u <- repository save user
      _ <- ZIO
        .when(user.status == Disabled):
          dispatcher.dispatch(AccountEvent.UserRegistered(u, w)).forkDaemon
    yield u

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

  val layer: URLayer[UserRepository & AccountEventDispatcher, UserServiceImpl] =
    ZLayer.fromZIO:
      for
        repository <- ZIO.service[UserRepository]
        dispatcher <- ZIO.service[AccountEventDispatcher]
      yield new UserServiceImpl(repository, dispatcher)
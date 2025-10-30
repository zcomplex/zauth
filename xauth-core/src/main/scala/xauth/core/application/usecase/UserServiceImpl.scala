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
import xauth.util.pagination.{PagedData, Pagination}
import zio.{Task, URLayer, ZIO, ZLayer}

import java.time.Instant

class UserServiceImpl(repository: UserRepository, dispatcher: AccountEventDispatcher) extends UserService:

  override def activate(code: String)(using w: Workspace): Task[Boolean] = ???

  override def childrenOf(id: Uuid)(using w: Workspace, p: Pagination): Task[PagedData[User]] = ???

  override def create(u: User)(using w: Workspace): Task[User] = ???

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

  override def create(username: String, password: String, description: Option[String], parentId: Option[Uuid], userInfo: UserInfo)(using w: Workspace): Task[User] = ???

  /**
   * Checks if hashed string has been encrypted for given salt and string.
   *
   * @param s  The salt.
   * @param ss The string to check with salt `s`.
   * @param hs The hashed string.
   * @return Returns `true` if `hs` has been encrypted by the given `s` salt and
   *         the string `s`, returns false otherwise.
   */
  override def checkWithSalt(s: String, ss: String, hs: String): Boolean = check(s + ss, hs)

  /**
   * Cyphers the string with a generated salt.
   *
   * @return Returns a [[Tuple2]] that contains generated salt and hashed string
   *         using `scrypt` encryption algorithm.
   */
  override def cryptWithSalt(s: String): (salt: String, hash: String) =
    val salt = (('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9')).random(79)
    val pass = scrypt(salt + s, 16, 16, 16) // 79 output bytes
    (salt, pass)

  override def findAll(implicit w: Workspace, p: Pagination): Task[PagedData[User]] = ???

  override def findById(id: Uuid)(using w: Workspace): Task[Option[User]] = ???

  override def findByUsername(username: String)(using w: Workspace): Task[Option[User]] = ???

  override def delete(id: Uuid)(using w: Workspace): Task[Boolean] = ???

  override def resetPassword(id: Uuid, password: String)(using w: Workspace): Task[Boolean] = ???

  override def trustAccount(u: User)(using w: Workspace): Task[Unit] = ???

  override def trustContact(c: String)(using w: Workspace): Task[Boolean] = ???

  override def updateApplications(id: Uuid, applications: AppInfo*)(using w: Workspace): Task[Option[User]] = ???

  override def updateRoles(id: Uuid, roles: AuthRole*)(using w: Workspace): Task[Option[User]] = ???

  override def updateStatusById(id: Uuid, status: AuthStatus)(using w: Workspace): Task[Option[User]] = ???

  override def updateStatusByUsername(u: String, status: AuthStatus)(using w: Workspace): Task[Option[User]] = ???

object UserServiceImpl:

  val layer: URLayer[UserRepository & AccountEventDispatcher, UserServiceImpl] =
    ZLayer.fromZIO:
      for
        repository <- ZIO.service[UserRepository]
        dispatcher <- ZIO.service[AccountEventDispatcher]
      yield new UserServiceImpl(repository, dispatcher)
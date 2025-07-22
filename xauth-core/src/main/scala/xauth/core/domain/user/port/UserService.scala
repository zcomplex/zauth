package xauth.core.domain.user.port

import xauth.core.common.model.{AuthRole, AuthStatus}
import xauth.core.domain.user.model.{AppInfo, User, UserInfo}
import xauth.core.domain.workspace.model.Workspace
import xauth.util.Uuid
import zio.Task

trait UserService:

  /**
   * Creates new user for the given workspace.
   *
   * @param username Username.
   * @param password User password that it will encrypted.
   * @param userInfo User information.
   * @return Returns a [[Task]] that boxes just created user.
   */
  def create(username: String, password: String,
             description: Option[String],
             parentId: Option[Uuid],
             userInfo: UserInfo,
             status: AuthStatus,
             applications: List[AppInfo],
             roles: AuthRole*)(using w: Workspace): Task[User]

//  todo: def create(u: User)(using w: Workspace): Task[User]
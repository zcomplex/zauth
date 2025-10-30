package xauth.core.domain.user.port

import xauth.core.common.model.{AuthRole, AuthStatus}
import xauth.core.domain.user.model.{AppInfo, User, UserInfo}
import xauth.core.domain.workspace.model.Workspace
import xauth.util.Uuid
import xauth.util.pagination.{PagedData, Pagination}
import zio.Task

trait UserService:

  /**
   * Activates the user account by an activation code.
   *
   * @param code The activation code.
   * @return Returns [[Future]] that boxes boolean if the operation has been
   *         completed without errors, boxes false otherwise.
   */
  def activate(code: String)(using w: Workspace): Task[Boolean]

  /**
   * Returns a paged list o users that have the parent-child relationship with the given parent.
   *
   * @param id The parent's user identifier.
   * @param w  The workspace in which to do the research.
   * @param p  The pagination data object.
   * @return Returns a future that wraps the paged users.
   */
  def childrenOf(id: Uuid)(using w: Workspace, p: Pagination): Task[PagedData[User]]

  /**
   * Creates new user for the given user object.
   *
   * @param u User to create.
   * @param w Current workspace.
   * @return Returns the created user.
   */
  def create(u: User)(using w: Workspace): Task[User]

  /**
   * Creates new user.
   *
   * @param username     Username.
   * @param password     User password that it will be encrypted.
   * @param description  User description.
   * @param parentId     Parent user identifier.
   * @param userInfo     User info.
   * @param status       Initial status.
   * @param applications List of application.
   * @param roles        Roles.
   * @param w            Workspace on which operate.
   * @return Returns a [[Task]] that boxes just created user.
   */
  def create(username: String, password: String,
             description: Option[String],
             parentId: Option[Uuid],
             userInfo: UserInfo,
             status: AuthStatus,
             applications: List[AppInfo],
             roles: AuthRole*)(using w: Workspace): Task[User]

  /**
   * Creates new user.
   *
   * @param username    Username.
   * @param password    User password that it will be encrypted.
   * @param description User description.
   * @param parentId    Parent user identifier.
   * @param userInfo    User info.
   * @param w           Workspace on which operate.
   * @return
   */
  def create(username: String, password: String,
             description: Option[String],
             parentId: Option[Uuid],
             userInfo: UserInfo)(using w: Workspace): Task[User]

  /**
   * Checks if hashed string has been encrypted for given salt and string.
   *
   * @param s  The salt.
   * @param ss The string to check with salt `s`.
   * @param hs The hashed string.
   * @return Returns `true` if `hs` has been encrypted by the given `s` salt and
   *         the the string `s`, returns false otherwise.
   */
  def checkWithSalt(s: String, ss: String, hs: String): Boolean

  /**
   * Cyphers the string with a generated salt.
   *
   * @return Returns a [[Tuple2]] that contains generated salt and hashed string.
   */
  def cryptWithSalt(s: String): (salt: String, hash: String)

  /**
   * Find all users and returns paged results.
   *
   * @param w Current workspace
   * @param p Pagination rules
   * @return Returns a future that boxes the paged result user list.
   */
  def findAll(implicit w: Workspace, p: Pagination): Task[PagedData[User]]

  /**
   * Searches and retrieves from persistence system the
   * user referred to the given identifier.
   *
   * @param id User identifier.
   * @return Returns non-empty [[Some(AuthUser)]] if the user was found.
   */
  def findById(id: Uuid)(using w: Workspace): Task[Option[User]]

  /**
   * Searches and retrieves user by its username.
   *
   * @param username Username.
   * @return Returns non-empty [[Some(User)]] if the user was found.
   */
  def findByUsername(username: String)(using w: Workspace): Task[Option[User]]

  /**
   * Deletes user from persistence system.
   *
   * @param id User identifier.
   * @return Returns `true` if the requested user has been deleted,
   *         returns false otherwise.
   */
  def delete(id: Uuid)(using w: Workspace): Task[Boolean]

  def resetPassword(id: Uuid, password: String)(using w: Workspace): Task[Boolean]

  /**
   * Trusts the associated user re-sending the activation code.
   *
   * @param u Account user.
   */
  def trustAccount(u: User)(using w: Workspace): Task[Unit]

  /**
   * Trusts the associated user contact by a contact trust code.
   *
   * @param c The contact trust code.
   * @return Returns [[Future]] that boxes boolean if the operation has been
   *         completed without errors, boxes false otherwise.
   */
  def trustContact(c: String)(using w: Workspace): Task[Boolean]

  def updateApplications(id: Uuid, applications: AppInfo*)(using w: Workspace): Task[Option[User]]

  def updateRoles(id: Uuid, roles: AuthRole*)(using w: Workspace): Task[Option[User]]

  def updateStatusById(id: Uuid, status: AuthStatus)(using w: Workspace): Task[Option[User]]

  def updateStatusByUsername(u: String, status: AuthStatus)(using w: Workspace): Task[Option[User]]
package xauth.infrastructure.user

import reactivemongo.api.bson.{BSONDocumentHandler, BSONHandler, BSONString, BSONValue, Macros}
import xauth.core.common.model.{AuthRole, AuthStatus, ContactType, Permission}
import xauth.core.domain.user.model.{AppInfo, User, UserContact, UserInfo}
import xauth.core.domain.user.port.UserRepository
import xauth.core.domain.workspace.model.Workspace
import xauth.infrastructure.mongo.DefaultMongoClient
import xauth.infrastructure.mongo.WorkspaceCollection.User as UserC
import xauth.infrastructure.mongo.bson.handler.given
import xauth.infrastructure.user.UserDo.*
import xauth.util.Uuid
import xauth.util.pagination.{PagedData, Pagination}
import zio.{Task, URLayer, ZIO, ZLayer}

import scala.util.{Success, Try}

class MongoUserRepository(mongo: DefaultMongoClient) extends UserRepository:

  private given authRoleBsonHandler: BSONHandler[AuthRole] = new BSONHandler[AuthRole]:
    override def readTry(b: BSONValue): Try[AuthRole] = b.asTry[BSONString] map { s => AuthRole.fromValue(s.value) }
    override def writeTry(s: AuthRole): Try[BSONValue] = Success(BSONString(s.value))

  private given permissionBsonHandler: BSONHandler[Permission] = new BSONHandler[Permission]:
    override def readTry(b: BSONValue): Try[Permission] = b.asTry[BSONString] map { s => Permission.fromValue(s.value) }
    override def writeTry(s: Permission): Try[BSONValue] = Success(BSONString(s.value))

  private given authStatusBsonHandler: BSONHandler[AuthStatus] = new BSONHandler[AuthStatus]:
    override def readTry(b: BSONValue): Try[AuthStatus] = b.asTry[BSONString] map { s => AuthStatus.fromValue(s.value) }
    override def writeTry(s: AuthStatus): Try[BSONValue] = Success(BSONString(s.value))

  private implicit val appInfoBsonHandler: BSONDocumentHandler[AppInfo] = Macros.handler[AppInfo]

  private given contactTypeBsonHandler: BSONHandler[ContactType] = new BSONHandler[ContactType]:
    override def readTry(b: BSONValue): Try[ContactType] = b.asTry[BSONString] map { s => ContactType.fromValue(s.value) }
    override def writeTry(s: ContactType): Try[BSONValue] = Success(BSONString(s.value))
  
  private implicit val userContactBsonHandler: BSONDocumentHandler[UserContact] = Macros.handler[UserContact]
  private implicit val userInfoBsonHandler: BSONDocumentHandler[UserInfo] = Macros.handler[UserInfo]

  private implicit val userBsonHandler: BSONDocumentHandler[UserDo] = Macros.handler[UserDo]

  /** Finds all users with pagination. */
  override def findAll(using p: Pagination): Task[PagedData[User]] = ???

  /** Deletes user by its identifier. */
  override infix def delete(id: Uuid)(using w: Workspace): Task[Boolean] = ???

  /** Finds all users. */
  override infix def findAll(using w: Workspace): Task[Seq[User]] = ???

  /** Finds user by its identifier. */
  override infix def find(id: Uuid)(using w: Workspace): Task[Option[User]] = ???

  /** Saves user on persistence system. */
  override infix def save(u: User)(using w: Workspace): Task[User] =
    mongo.collection(UserC) flatMap:
      c => ZIO.fromFuture:
        implicit _ => c.insert.one(u.fromDomain) map { _ => u }

object MongoUserRepository:

  lazy val layer: URLayer[DefaultMongoClient, MongoUserRepository] =
    ZLayer.fromZIO:
      ZIO.service[DefaultMongoClient] map:
        mongo => new MongoUserRepository(mongo)
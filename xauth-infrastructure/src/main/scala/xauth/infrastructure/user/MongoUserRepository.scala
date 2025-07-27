package xauth.infrastructure.user

import xauth.core.domain.user.model.User
import xauth.core.domain.user.port.UserRepository
import xauth.core.domain.workspace.model.Workspace
import xauth.infrastructure.mongo.DefaultMongoClient
import xauth.infrastructure.mongo.WorkspaceCollection.User as UserC
import xauth.infrastructure.user.UserDo.*
import xauth.util.Uuid
import xauth.util.pagination.{PagedData, Pagination}
import zio.{Task, URLayer, ZIO, ZLayer}

class MongoUserRepository(mongo: DefaultMongoClient) extends UserRepository:

  import bson.handler.given

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
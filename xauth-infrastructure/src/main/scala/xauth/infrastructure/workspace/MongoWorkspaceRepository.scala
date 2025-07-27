package xauth.infrastructure.workspace

import reactivemongo.api.bson.*
import reactivemongo.api.indexes.IndexType.Ascending
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.api.{Cursor, ReadPreference}
import xauth.core.domain.workspace.model.*
import xauth.core.domain.workspace.port.WorkspaceRepository
import xauth.infrastructure.mongo.SystemCollection.Workspace as WorkspaceC
import xauth.infrastructure.mongo.WorkspaceCollection.{Invitation, User}
import xauth.infrastructure.mongo.{DefaultMongoClient, WorkspaceCollection}
import xauth.infrastructure.workspace.WorkspaceDo.*
import xauth.util.Uuid
import xauth.util.pagination.{PagedData, Pagination}
import zio.*

class MongoWorkspaceRepository(mongo: DefaultMongoClient) extends WorkspaceRepository:

  import bson.handler.given
  import xauth.infrastructure.mongo.bson.handler.uuidBsonHandler

  private def find(s: BSONDocument): Task[Option[Workspace]] =
    mongo.collection(WorkspaceC) flatMap:
      c => ZIO.fromFuture(implicit _ => c.find(s).one[WorkspaceDo].map(_.map(_.toDomain)))

  override def findBySlug(s: String): Task[Option[Workspace]] =
    find:
      BSONDocument("slug" -> s)

  /** Deletes workspace by its identifier. */
  override def delete(id: Uuid): Task[Boolean] =
    mongo.collection(WorkspaceC) flatMap:
      c => ZIO.fromFuture:
        implicit _ => c
          .delete
          .one:
            BSONDocument("_id" -> id)
          .map(_.n > 0)

  /** Finds all workspaces.0645263160 */
  override def findAll: Task[Seq[Workspace]] =
    for
      c <- mongo.collection(WorkspaceC)
      s <- ZIO.fromFuture:
        implicit ec => c
          .find(BSONDocument.empty)
          .cursor[WorkspaceDo]()
          .collect[List](-1, Cursor.FailOnError())
    yield s.map(_.toDomain)

  /** Finds workspace by its identifier. */
  override def find(id: Uuid): Task[Option[Workspace]] =
    find:
      BSONDocument("_id" -> id)

  /** Saves workspace on persistence system. */
  override def save(t: Workspace): Task[Workspace] =
    mongo.collection(WorkspaceC) flatMap:
      c => ZIO.fromFuture:
        implicit _ => c.insert.one(t.fromDomain) map { _ => t }

  /** Finds all workspaces with pagination. */
  override def findAll(using p: Pagination): Task[PagedData[Workspace]] =
    for
      c       <- mongo.collection(WorkspaceC)

      selector = BSONDocument.empty

      count   <- ZIO.fromFuture:
        implicit ec => c.count(Some(selector))

      results <- ZIO.fromFuture:
        implicit ec => c
          .find(selector)
          .skip(p.offset)
          .cursor[WorkspaceDo](ReadPreference.Primary)
          .collect[Seq](p.size, Cursor.FailOnError())

    yield p.paginate(results.map(_.toDomain), count.toInt)

  override def configureIndexes(using w: Workspace): Task[Boolean] =
    // creates index for the specified collection and field
    def setup(cType: WorkspaceCollection, key: String, indexType: IndexType): Task[Boolean] =
      for
        c <- mongo.collection(cType)(using w)
        b <- ZIO
          .fromFuture:
            implicit ec => c.indexesManager.ensure(Index(key = (key -> indexType) :: Nil, unique = true))
          .tap:
            case true  => ZIO logInfo    s"created index ${cType.name}.$key"
            case false => ZIO logWarning s"index creation failed for ${cType.name}.$key"
          .tapError:
            t => ZIO logError s"index creation failed for ${cType.name}.$key: ${t.getMessage}"
          .orElseSucceed:
            false
      yield b

    for
      b1 <- setup(User, "username", Ascending)
      b2 <- setup(User, "userInfo.contacts.value", Ascending)
      b3 <- setup(Invitation, "email", Ascending)
    yield b1 && b2 && b3

object MongoWorkspaceRepository:
  
  val layer: URLayer[DefaultMongoClient, WorkspaceRepository] =
    ZLayer.fromZIO:
      ZIO.service[DefaultMongoClient] map:
        new MongoWorkspaceRepository(_)
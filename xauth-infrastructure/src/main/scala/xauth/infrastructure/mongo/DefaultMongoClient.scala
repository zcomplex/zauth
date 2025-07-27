package xauth.infrastructure.mongo

import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.{DB, MongoConnection}
import xauth.core.domain.configuration.model.Configuration
import xauth.util.Uuid
import xauth.util.mongo.*
import zio.*
import zio.stm.TMap

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

class DefaultMongoClient(databases: TMap[Uuid, DB], driver: Driver[MongoConnection, DB])
  extends MongoClient[MongoConnection, Uuid, DB, BSONCollection](databases, driver)
    with SystemColl[SystemCollection, Uuid, BSONCollection]
    with WorkspaceColl[WorkspaceCollection, Uuid, Workspace[Uuid], BSONCollection]:

  override def collection(t: SystemCollection): IO[MongoError, BSONCollection] =
    database(Uuid.Zero).map(_.collection[BSONCollection](t.name))

  override def collections(t: SystemCollection): IO[MongoError, Seq[(Uuid, BSONCollection)]] =
    databases.toList.commit map:
      _.map:
        case (uuid, db) => uuid -> db.collection[BSONCollection](t.name)

  override def collection(t: WorkspaceCollection)(using w: Workspace[Uuid]): IO[MongoError, BSONCollection] =
    database(w.workspaceId).map(_.collection[BSONCollection](t.name))

  override def collections(t: WorkspaceCollection)(using ws: Seq[Workspace[Uuid]]): IO[MongoError, Seq[(Workspace[Uuid], BSONCollection)]] =
    ZIO.foreach(ws)(w => database(w.workspaceId).map(c => w -> c.collection[BSONCollection](t.name)))

  override def close(using w: Workspace[Uuid]): IO[MongoError, Unit] =
    database(w.workspaceId)
      .flatMap:
        db =>
          ZIO
            .fromFuture(implicit ec => db.connection.close()(using FiniteDuration(1, TimeUnit.SECONDS)))
            .mapError(t => MongoError.AccessFailed(t.getMessage))
      .unit *> databases.delete(w.workspaceId).commit

object DefaultMongoClient:

  /** Creates a client with a connection to the root workspace. */
  val layer: ZLayer[Configuration & Driver[MongoConnection, DB], Nothing, DefaultMongoClient] =

    val effect = for
      cnf    <- ZIO.service[Configuration]
      driver <- ZIO.service[Driver[MongoConnection, DB]]
      dbs    <- TMap.empty[Uuid, DB].commit
      client <- ZIO succeed new DefaultMongoClient(dbs, driver)
      // Connecting to the system workspace
      _      <- client.connect(Uuid.Zero -> cnf.init.workspace.configuration.database.uri).orDie
    yield client

    ZLayer.scoped:
      ZIO
        .acquireRelease(effect):
          c => ZIO
            .logInfo(s"releasing mongodb connection...") *> c
            .close()
            .catchAll(e => ZIO.logWarning(s"unable to release mongodb client: $e"))
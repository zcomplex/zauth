package xauth.infrastructure.client

import reactivemongo.api.bson.BSONDocument
import xauth.core.domain.client.model.Client
import xauth.core.domain.client.port.ClientRepository
import xauth.core.domain.workspace.model.Workspace
import xauth.infrastructure.client.ClientDo.*
import xauth.infrastructure.mongo.DefaultMongoClient
import xauth.infrastructure.mongo.WorkspaceCollection.Client as ClientC
import zio.{Task, URLayer, ZIO, ZLayer}

class MongoClientRepository(mongo: DefaultMongoClient) extends ClientRepository:

  import bson.handler.given

  /** Deletes entity by its identifier. */
  override infix def delete(id: String)(using w: Workspace): Task[Boolean] = ???

  /** Finds all entities. */
  override infix def findAll(using w: Workspace): Task[Seq[Client]] = ???

  /** Finds entity by its identifier. */
  override infix def find(id: String)(using w: Workspace): Task[Option[Client]] =
    mongo.collection(ClientC) flatMap:
      c =>
        val s = BSONDocument("_id" -> id)
        ZIO.fromFuture(implicit _ => c.find(s).one[ClientDo].map(_.map(_.toDomain)))

  /** Saves entity on persistence system. */
  override infix def save(c: Client)(using w: Workspace): Task[Client] =
    mongo.collection(ClientC).flatMap:
      collection => ZIO.fromFuture:
        implicit _ => collection.insert.one(c.fromDomain) map { _ => c }

object MongoClientRepository:

  lazy val layer: URLayer[DefaultMongoClient, MongoClientRepository] =
    ZLayer.fromZIO:
      ZIO.service[DefaultMongoClient] map:
        new MongoClientRepository(_)
  
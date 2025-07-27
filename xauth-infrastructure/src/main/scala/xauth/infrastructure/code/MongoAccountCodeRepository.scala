package xauth.infrastructure.code

import xauth.core.domain.code.model.AccountCode
import xauth.core.domain.code.port.AccountCodeRepository
import xauth.core.domain.workspace.model.Workspace
import xauth.infrastructure.code.AccountCodeDo.*
import xauth.infrastructure.mongo.DefaultMongoClient
import xauth.infrastructure.mongo.WorkspaceCollection.Code as CodeC
import zio.{Task, URLayer, ZIO, ZLayer}

private final class MongoAccountCodeRepository(mongo: DefaultMongoClient) extends AccountCodeRepository:

  import bson.given

  /** Deletes account code by code. */
  override infix def delete(code: String)(using w: Workspace): Task[Boolean] = ???

  /** Finds all account codes. */
  override infix def findAll(using w: Workspace): Task[Seq[AccountCode]] = ???

  /** Finds account code by code. */
  override infix def find(code: String)(using w: Workspace): Task[Option[AccountCode]] = ???

  /** Saves account code on persistence system. */
  override infix def save(a: AccountCode)(using w: Workspace): Task[AccountCode] =
    mongo.collection(CodeC) flatMap:
      c => ZIO.fromFuture:
        implicit _ => c.insert.one(a.fromDomain) map { _ => a }

object MongoAccountCodeRepository:
  
  lazy val layer: URLayer[DefaultMongoClient, AccountCodeRepository] =
    ZLayer.fromZIO:
      ZIO.service[DefaultMongoClient] map:
        mongo => new MongoAccountCodeRepository(mongo)
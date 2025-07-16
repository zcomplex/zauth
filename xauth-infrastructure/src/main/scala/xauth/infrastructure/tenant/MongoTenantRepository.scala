package xauth.infrastructure.tenant

import reactivemongo.api.bson.*
import reactivemongo.api.{Cursor, ReadPreference}
import xauth.core.domain.tenant.model.Tenant
import xauth.core.domain.tenant.port.TenantRepository
import xauth.infrastructure.mongo.DefaultMongoClient
import xauth.infrastructure.mongo.SystemCollection.Tenant as TenantC
import xauth.infrastructure.mongo.bson.handler.given
import xauth.infrastructure.tenant.TenantDo.*
import xauth.util.Uuid
import xauth.util.pagination.{PagedData, Pagination}
import zio.*

import scala.util.{Success, Try}

class MongoTenantRepository(mongo: DefaultMongoClient) extends TenantRepository:

  private implicit val tenantBsonHandler: BSONDocumentHandler[TenantDo] = Macros.handler[TenantDo]

  private def find(s: BSONDocument) =
    mongo.collection(TenantC) flatMap:
      c => ZIO.fromFuture(implicit _ => c.find(s).one[TenantDo].map(_.map(_.toDomain)))

  override def findBySlug(s: String): Task[Option[Tenant]] =
    find:
      BSONDocument("slug" -> s)

  /** Deletes tenant by its identifier. */
  override def delete(id: Uuid): Task[Boolean] =
    mongo.collection(TenantC) flatMap:
      c => ZIO.fromFuture:
        implicit _ => c
          .delete
          .one:
            BSONDocument("_id" -> id)
          .map(_.n > 0)

  /** Finds all tenants. */
  override def findAll: Task[Seq[Tenant]] =
    for
      c <- mongo.collection(TenantC)
      s <- ZIO.fromFuture:
        implicit ec =>
          c
            .find(BSONDocument.empty)
            .cursor[TenantDo]()
            .collect[List](-1, Cursor.FailOnError())
    yield s.map(_.toDomain)

  /** Finds tenant by its identifier. */
  override def find(id: Uuid): Task[Option[Tenant]] =
    find:
      BSONDocument("_id" -> id)

  /** Saves tenant on persistence system. */
  override def save(t: Tenant): Task[Tenant] =
    mongo.collection(TenantC) flatMap:
      c => ZIO.fromFuture:
        implicit _ => c.insert.one(t.fromDomain) map { _ => t }

  /** Finds all tenants with pagination. */
  override def findAll(using p: Pagination): Task[PagedData[Tenant]] =
    for
      c       <- mongo.collection(TenantC)

      selector = BSONDocument.empty

      count   <- ZIO.fromFuture:
        implicit ec => c.count(Some(selector))

      results <- ZIO.fromFuture:
        implicit ec => c
          .find(selector)
          .skip(p.offset)
          .cursor[TenantDo](ReadPreference.Primary)
          .collect[Seq](p.size, Cursor.FailOnError())

    yield p.paginate(results.map(_.toDomain), count.toInt)

object MongoTenantRepository:

  lazy val layer: URLayer[DefaultMongoClient, MongoTenantRepository] =
    ZLayer.fromZIO:
      ZIO.service[DefaultMongoClient] map:
        mongo => new MongoTenantRepository(mongo)
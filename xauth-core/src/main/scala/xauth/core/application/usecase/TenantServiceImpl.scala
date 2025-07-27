package xauth.core.application.usecase

import xauth.core.domain.tenant.model.Tenant
import xauth.core.domain.tenant.port.{TenantRepository, TenantService}
import xauth.util.Uuid
import zio.{Task, URLayer, ZIO, ZLayer}

import java.time.Instant

class TenantServiceImpl(repository: TenantRepository) extends TenantService:
  /**
   * Searches and retrieves from persistence system the
   * tenant referred to the given identifier.
   */
  override def findById(id: Uuid): Task[Option[Tenant]] = ???

  /**
   * Searches and retrieves from persistence system the
   * tenant referred to the given slug.
   */
  override def findBySlug(slug: String): Task[Option[Tenant]] = ???

  /** Retrieves all configured tenants. */
  override def findAll: Task[Seq[Tenant]] = ???

  /** Creates the system default tenant. */
  override def createSystemTenant: Task[Tenant] = {
    val now = Instant.now

    val tenant = Tenant(
      id = Uuid.Zero,
      slug = "root",
      description = "system default",
      workspaceIds = Uuid.Zero :: Nil,
      createdAt = now,
      createdBy = Uuid.Zero,
      updatedAt = now,
      updatedBy = Uuid.Zero
    )

    repository.save(tenant)
  }

  /** Creates new Tenant. */
  override def create(slug: String, description: String): Task[Tenant] = ???

  /** Updates the given tenant */
  override def update(t: Tenant): Task[Tenant] = ???

object TenantServiceImpl:
  
  val layer: URLayer[TenantRepository, TenantServiceImpl] =
    ZLayer.fromZIO:
      ZIO.service[TenantRepository] map:
        new TenantServiceImpl(_)
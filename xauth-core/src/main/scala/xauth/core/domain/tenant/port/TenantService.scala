package xauth.core.domain.tenant.port

import xauth.core.domain.tenant.model.Tenant
import xauth.util.Uuid
import zio.Task

trait TenantService:

  /**
   * Searches and retrieves from persistence system the
   * tenant referred to the given identifier.
   */
  def findById(id: Uuid): Task[Option[Tenant]]

  /**
   * Searches and retrieves from persistence system the
   * tenant referred to the given slug.
   */
  def findBySlug(slug: String): Task[Option[Tenant]]

  /** Retrieves all configured tenants. */
  def findAll: Task[Seq[Tenant]]

  /** Creates the system default tenant. */
  def createSystemTenant: Task[Tenant]
  
  /** Creates new Tenant. */
  def create(slug: String, description: String): Task[Tenant]
  
  /** Updates the given tenant */
  def update(t: Tenant): Task[Tenant]
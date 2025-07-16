package xauth.core.domain.tenant.port

import xauth.core.domain.tenant.model.Tenant
import xauth.core.spi.{PagedRepository, Repository}
import xauth.util.Uuid
import zio.Task

trait TenantRepository extends Repository[Tenant, Uuid] with PagedRepository[Tenant]:
  
  /**
   * Searches and retrieves from persistence system the
   * tenant referred to the given slug.
   *
   * @param s Tenant slug.
   * @return Returns non-empty option if the tenant was found.
   */
  def findBySlug(s: String): Task[Option[Tenant]]
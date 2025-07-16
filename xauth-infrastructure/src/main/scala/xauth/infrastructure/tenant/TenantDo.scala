package xauth.infrastructure.tenant

import reactivemongo.api.bson.Macros.Annotations.Key
import xauth.core.domain.tenant.model.Tenant
import xauth.util.Uuid

import java.time.Instant

case class TenantDo
(
  @Key("_id")
  id: Uuid,
  slug: String,
  description: String,
  workspaceIds: Seq[Uuid] = Seq.empty,
  registeredAt: Instant,
  updatedAt: Instant
)

object TenantDo:

  extension (t: Tenant)
    def fromDomain: TenantDo =
      TenantDo(
        id = t.id,
        slug = t.slug,
        description = t.description,
        workspaceIds = t.workspaceIds,
        registeredAt = t.registeredAt,
        updatedAt = t.updatedAt
      )

  extension (t: TenantDo)
    def toDomain: Tenant =
      Tenant(
        id = t.id,
        slug = t.slug,
        description = t.description,
        workspaceIds = t.workspaceIds,
        registeredAt = t.registeredAt,
        updatedAt = t.updatedAt
      )
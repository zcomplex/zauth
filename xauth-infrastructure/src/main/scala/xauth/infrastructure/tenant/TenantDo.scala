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
  createdAt: Instant,
  createdBy: Uuid,
  updatedAt: Instant,
  updatedBy: Uuid
)

object TenantDo:

  extension (t: Tenant)
    def fromDomain: TenantDo =
      TenantDo(
        id = t.id,
        slug = t.slug,
        description = t.description,
        workspaceIds = t.workspaceIds,
        createdAt = t.createdAt,
        createdBy = t.createdBy,
        updatedAt = t.updatedAt,
        updatedBy = t.updatedBy
      )

  extension (t: TenantDo)
    def toDomain: Tenant =
      Tenant(
        id = t.id,
        slug = t.slug,
        description = t.description,
        workspaceIds = t.workspaceIds,
        createdAt = t.createdAt,
        createdBy = t.createdBy,
        updatedAt = t.updatedAt,
        updatedBy = t.updatedBy
      )
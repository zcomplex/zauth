package xauth.infrastructure.workspace

import reactivemongo.api.bson.Macros.Annotations.Key
import xauth.core.domain.workspace.model.{Company, Workspace, WorkspaceConf, WorkspaceStatus}
import xauth.util.Uuid

import java.time.Instant

/** Workspace Data Object */
case class WorkspaceDo
(
  @Key("_id")
  id: Uuid,
  tenantId: Uuid,
  slug: String,
  description: String,
  company: Company,
  status: WorkspaceStatus,
  configuration: WorkspaceConf,
  createdAt: Instant,
  createdBy: Uuid,
  updatedAt: Instant,
  updatedBy: Uuid
)

object WorkspaceDo:

  extension (w: Workspace)
    def fromDomain: WorkspaceDo =
      WorkspaceDo(
        id = w.id,
        tenantId = w.tenantId,
        slug = w.slug,
        description = w.description,
        company = w.company,
        status = w.status,
        configuration = w.configuration,
        createdAt = w.createdAt,
        createdBy = w.createdBy,
        updatedAt = w.updatedAt,
        updatedBy = w.updatedBy
      )

  extension (w: WorkspaceDo)
    def toDomain: Workspace =
      Workspace(
        id = w.id,
        tenantId = w.tenantId,
        slug = w.slug,
        description = w.description,
        company = w.company,
        status = w.status,
        configuration = w.configuration,
        createdAt = w.createdAt,
        createdBy = w.createdBy,
        updatedAt = w.updatedAt,
        updatedBy = w.updatedBy
      )
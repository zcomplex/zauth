package xauth.infrastructure.workspace

import reactivemongo.api.bson.Macros.Annotations.Key
import xauth.core.domain.workspace.model.{Company, Workspace, WorkspaceConf, WorkspaceStatus}
import xauth.util.Uuid
import xauth.util.time.ZonedDate

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
  registeredAt: ZonedDate,
  updatedAt: ZonedDate
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
        registeredAt = w.registeredAt,
        updatedAt = w.updatedAt,
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
        registeredAt = w.registeredAt,
        updatedAt = w.updatedAt,
      )
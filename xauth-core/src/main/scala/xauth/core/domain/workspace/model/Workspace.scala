package xauth.core.domain.workspace.model

import xauth.util.Uuid
import xauth.util.time.ZonedDate

case class Workspace
(
  id: Uuid,
  tenantId: Uuid,
  slug: String,
  description: String,
  status: WorkspaceStatus,
  configuration: WorkspaceConfiguration,
  registeredAt: ZonedDate,
  updatedAt: ZonedDate
) extends xauth.util.mongo.Workspace[Uuid]:
  override lazy val workspaceId: Uuid = id
  override lazy val connectionUri: String = configuration.database.uri
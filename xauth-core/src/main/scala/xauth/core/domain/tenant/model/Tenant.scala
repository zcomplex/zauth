package xauth.core.domain.tenant.model

import xauth.util.Uuid

import java.time.Instant

case class Tenant
(
  id: Uuid,
  slug: String,
  description: String,
  workspaceIds: Seq[Uuid] = Seq.empty,
  registeredAt: Instant,
  updatedAt: Instant
)
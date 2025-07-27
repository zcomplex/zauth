package xauth.core.domain.client.model

import xauth.util.Uuid

import java.time.Instant

case class Client
(
  id: String,
  secret: String,
  createdAt: Instant,
  createdBy: Uuid,
  updatedAt: Instant,
  updatedBy: Uuid
)
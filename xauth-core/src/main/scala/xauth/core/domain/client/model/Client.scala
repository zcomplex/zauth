package xauth.core.domain.client.model

import java.time.Instant

case class Client
(
  id: String,
  secret: String,
  registeredAt: Instant,
  updatedAt: Instant
)

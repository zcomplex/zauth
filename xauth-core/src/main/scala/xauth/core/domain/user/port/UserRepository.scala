package xauth.core.domain.user.port

import xauth.core.domain.user.model.User
import xauth.core.spi.{PagedRepository, WorkspaceRepository}
import xauth.util.Uuid

trait UserRepository extends WorkspaceRepository[User, Uuid] with PagedRepository[User]
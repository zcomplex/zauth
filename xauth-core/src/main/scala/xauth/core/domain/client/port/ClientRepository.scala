package xauth.core.domain.client.port

import xauth.core.domain.client.model.Client
import xauth.core.spi.{Repository, WorkspaceRepository}

trait ClientRepository extends WorkspaceRepository[Client, String]
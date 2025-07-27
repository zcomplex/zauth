package xauth.core.domain.code.port

import xauth.core.domain.code.model.AccountCode
import xauth.core.spi.WorkspaceRepository

/** Allow to read and write account codes. */
trait AccountCodeRepository extends WorkspaceRepository[AccountCode, String]
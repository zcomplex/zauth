package xauth.core.spi

import xauth.core.domain.workspace.model.Workspace
import xauth.util.pagination.{PagedData, Pagination}
import zio.Task

trait PagedWorkspaceRepository[T]:

  /** Finds all with pagination. */
  def findAll(using p: Pagination, w: Workspace): Task[PagedData[T]]
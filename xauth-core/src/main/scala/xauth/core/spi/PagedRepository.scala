package xauth.core.spi

import xauth.util.pagination.{PagedData, Pagination}
import zio.Task

trait PagedRepository[T]:

  /** Finds all with pagination. */
  def findAll(using p: Pagination): Task[PagedData[T]]
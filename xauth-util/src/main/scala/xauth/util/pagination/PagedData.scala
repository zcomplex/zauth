package xauth.util.pagination

case class PagedData[T]
(
  page: Int,
  pageSize: Int,
  pageCount: Int,
  totalPages: Int,
  totalCount: Int,
  elements: Seq[T]
)
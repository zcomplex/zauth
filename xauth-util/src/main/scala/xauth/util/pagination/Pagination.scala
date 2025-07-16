package xauth.util.pagination

class Pagination(val page: Int, val size: Int, val offset: Int):
  def paginate[T](s: Seq[T], totalCount: Int): PagedData[T] =
    PagedData(
      page = page,
      pageSize = size, pageCount = s.size,
      totalPages = if (totalCount < size) 1 else Math.round(totalCount.toFloat / size.toFloat), totalCount = totalCount,
      elements = s
    )

object Pagination:
  private val DefaultPage: Int = 1
  private val DefaultPageSize: Int = 10
  
  trait Request:
    def parameter(p: String): Option[String]

  def apply(page: Int, size: Int)(using spec: OffsetSpec): Pagination =
    new Pagination(
      page,
      size,
      (page - 1) * spec.offset(page) * size
    )

  def fromRequest(r: Request)(using spec: OffsetSpec): Pagination =
    val page = r
      .parameter("page")
      .map(_.toInt)
      .filter(_ > 0) getOrElse DefaultPage
    val size = r
      .parameter("size")
      .map(_.toInt)
      .filter(_ > 0) getOrElse DefaultPageSize
    apply(page, size)
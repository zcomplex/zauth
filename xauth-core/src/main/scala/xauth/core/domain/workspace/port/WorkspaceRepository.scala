package xauth.core.domain.workspace.port

import xauth.core.domain.workspace.model.Workspace
import xauth.core.spi.{PagedRepository, Repository}
import xauth.util.Uuid
import zio.Task

trait WorkspaceRepository extends Repository[Workspace, Uuid] with PagedRepository[Workspace]:

  /**
   * Searches and retrieves from persistence system the
   * workspace referred to the given slug.
   *
   * @param s Workspace slug.
   * @return Returns non-empty option if the workspace was found.
   */
  def findBySlug(s: String): Task[Option[Workspace]]

  def configureIndexes(implicit w: Workspace): Task[Boolean]
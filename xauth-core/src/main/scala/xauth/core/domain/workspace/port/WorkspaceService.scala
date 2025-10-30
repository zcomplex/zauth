package xauth.core.domain.workspace.port

import xauth.core.domain.workspace.model.{Workspace, WorkspaceConf, WorkspaceInit}
import xauth.util.Uuid
import zio.Task

trait WorkspaceService:

  /**
   * Searches and retrieves from persistence system the
   * workspace referred to the given identifier.
   */
  infix def findById(id: Uuid): Task[Option[Workspace]]

  /**
   * Searches and retrieves from persistence system the
   * workspace referred to the given slug.
   */
  infix def findBySlug(slug: String): Task[Option[Workspace]]

  /** Retrieves all configured workspaces. */
  def findAll: Task[Seq[Workspace]]

  /** Creates system default workspace. */
  def createSystemWorkspace: Task[Workspace]

  /** Creates new workspace. */
  def create(tenantId: Uuid, slug: String, desc: String, conf: WorkspaceConf, init: WorkspaceInit): Task[Workspace]

  /** Updates the given workspace. */
  infix def update(w: Workspace): Task[Workspace]
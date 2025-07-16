package xauth.core.domain.workspace.port

import xauth.core.domain.workspace.model.{Workspace, WorkspaceConfiguration, WorkspaceInit}
import xauth.util.Uuid
import zio.Task

trait WorkspaceService:

  /**
   * Searches and retrieves from persistence system the
   * workspace referred to the given identifier.
   */
  def findById(id: Uuid): Task[Option[Workspace]]

  /**
   * Searches and retrieves from persistence system the
   * workspace referred to the given slug.
   */
  def findBySlug(slug: String): Task[Option[Workspace]]

  /** Retrieves all configured workspaces. */
  def findAll: Task[Seq[Workspace]]

  /** Creates system default workspace. */
  def createSystemWorkspace(applications: Seq[String]): Task[Workspace]

  /** Creates new workspace. */
  def create(tenantId: Uuid, slug: String, desc: String, conf: WorkspaceConfiguration, init: WorkspaceInit): Task[Workspace]

  /** Updates the given workspace. */
  def update(w: Workspace): Task[Workspace]
package xauth.core.spi

import xauth.core.domain.workspace.model.Workspace
import zio.Task

trait WorkspaceRepository[T, Id]:

  /** Deletes entity by its identifier. */
  infix def delete(id: Id)(using w: Workspace): Task[Boolean]

  /** Finds all entities. */
  infix def findAll(using w: Workspace): Task[Seq[T]]

  /** Finds entity by its identifier. */
  infix def find(id: Id)(using w: Workspace): Task[Option[T]]

  /** Saves entity on persistence system. */
  infix def save(e: T)(using w: Workspace): Task[T]
package xauth.core.spi

import zio.Task

trait Repository[T, Id]:

  /** Deletes entity by its identifier. */
  infix def delete(id: Id): Task[Boolean]

  /** Finds all entities. */
  infix def findAll: Task[Seq[T]]

  /** Finds entity by its identifier. */
  infix def find(id: Id): Task[Option[T]]

  /** Saves entity on persistence system. */
  infix def save(e: T): Task[T]
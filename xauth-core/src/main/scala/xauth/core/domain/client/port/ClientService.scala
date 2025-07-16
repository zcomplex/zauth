package xauth.core.domain.client.port

import xauth.core.domain.client.model.Client
import xauth.core.domain.workspace.model.Workspace
import zio.Task

/** Service to handle authentication client for basic-authentication. */
trait ClientService:

  /** Finds a client by identifier for the given workspace. */
  def find(id: String)(using w: Workspace): Task[Option[Client]]

  /** Finds all clients for the given workspace. */
  def findAll(using w: Workspace): Task[Seq[Client]]

  /** Creates a new client for the given workspace. */
  def create(id: String, secret: String)(using w: Workspace): Task[Client]

  /** Updates a client for the given workspace. */
  def update(c: Client)(using w: Workspace): Task[Option[Client]]

  /** Deletes a client for the given workspace. */
  def delete(id: String)(using w: Workspace): Task[Boolean]
  
  // todo: evaluate to add support for api keys
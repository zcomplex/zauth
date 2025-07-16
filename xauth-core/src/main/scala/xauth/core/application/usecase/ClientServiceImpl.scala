package xauth.core.application.usecase

import xauth.core.domain.client.model.Client
import xauth.core.domain.client.port.{ClientRepository, ClientService}
import xauth.core.domain.workspace.model.Workspace
import zio.{Task, URLayer, ZIO, ZLayer}
import xauth.util.ext.md5

import java.time.Instant

class ClientServiceImpl(repository: ClientRepository) extends ClientService:

  /** Finds a client by identifier for the given workspace. */
  def find(id: String)(using w: Workspace): Task[Option[Client]] = ???

  /** Finds all clients for the given workspace. */
  def findAll(using w: Workspace): Task[Seq[Client]] = ???

  /** Creates a new client for the given workspace. */
  def create(id: String, secret: String)(using w: Workspace): Task[Client] =

    val now = Instant.now
    val client = Client(id, secret.md5, now, now)
  
    for
      // todo: add existence checks
      c <- repository save client
    yield c

  /** Updates a client for the given workspace. */
  def update(c: Client)(using w: Workspace): Task[Option[Client]] = ???

  /** Deletes a client for the given workspace. */
  def delete(id: String)(using w: Workspace): Task[Boolean] = ???

object ClientServiceImpl:

  lazy val layer: URLayer[ClientRepository, ClientServiceImpl] =
    ZLayer.fromZIO:
      ZIO.service[ClientRepository] map:
        new ClientServiceImpl(_)
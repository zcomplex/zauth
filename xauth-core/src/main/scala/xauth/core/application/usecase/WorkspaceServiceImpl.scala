package xauth.core.application.usecase

import xauth.core.application.usecase.WorkspaceServiceImpl.{copyKeyGenerator, generateKeyPair}
import xauth.core.domain.configuration.model.Configuration
import xauth.core.domain.workspace.model.WorkspaceStatus.Enabled
import xauth.core.domain.workspace.model.{Company, Workspace, WorkspaceConf, WorkspaceInit}
import xauth.core.domain.workspace.port.{WorkspaceRepository, WorkspaceService}
import xauth.util.Uuid
import zio.{Task, URLayer, ZIO, ZLayer}

import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.{Files, Path, Paths}
import java.time.Instant

class WorkspaceServiceImpl(repository: WorkspaceRepository, registry: WorkspaceRegistry, conf: Configuration) extends WorkspaceService:

  override infix def findById(id: Uuid): Task[Option[Workspace]] =
    repository.find(id)

  override infix def findBySlug(slug: String): Task[Option[Workspace]] =
    repository.findBySlug(slug)

  /** Retrieves all configured workspaces. */
  override def findAll: Task[Seq[Workspace]] =
    repository.findAll

  /** Creates system default workspace. */
  override def createSystemWorkspace: Task[Workspace] =

    val now = Instant.now

    val workspace = Workspace(
      id = Uuid.Zero,
      tenantId = Uuid.Zero,
      slug = conf.init.workspace.slug,
      description = conf.init.workspace.description,
      company = Company(
        name = conf.init.workspace.company.name,
        description = conf.init.workspace.company.description
      ),
      status = Enabled,
      configuration = conf.init.workspace.configuration,
      createdAt = now,
      createdBy = Uuid.Zero,
      updatedAt = now,
      updatedBy = Uuid.Zero
    )

    for
      // writing system default workspace
      w <- repository save workspace
      // configuring indexes
      _ <- repository.configureIndexes(using workspace)
      // copying keygen
      _ <- copyKeyGenerator(using conf)
      // generating system workspace key pair
      _ <- generateKeyPair(using workspace, conf)
      // registering just created workspace
      _ <- registry register w
    yield w

  /** Creates new workspace. */
  override def create(tenantId: Uuid, slug: String, desc: String, conf: WorkspaceConf, init: WorkspaceInit): Task[Workspace] = ???

  /** Updates the given workspace. */
  override infix def update(w: Workspace): Task[Workspace] = ???

object WorkspaceServiceImpl:

  import scala.sys.process.*

  val layer: URLayer[WorkspaceRegistry & WorkspaceRepository & Configuration, WorkspaceServiceImpl] =
    ZLayer.fromZIO:
      for
        r <- ZIO.service[WorkspaceRepository]
        g <- ZIO.service[WorkspaceRegistry]
        c <- ZIO.service[Configuration]
      yield
        new WorkspaceServiceImpl(r, g, c)

  def copyKeyGenerator(using c: Configuration): Task[Unit] =
    for
      // base configuration path
      confPath <- ZIO attempt Paths.get(c.confPath)
      // key pairs generation script path
      srcKeygenPath <- ZIO attempt:
        Paths
          .get(".")
          .resolve("script")
          .resolve("keygen.sh")
      // key pairs generation script path
      dstKeygenPath <- ZIO attempt:
        confPath
          .resolve("script")
          .resolve("keygen.sh")
      _ <- ZIO
        .when(!Files.exists(dstKeygenPath)):
          ZIO
            .attempt:
              // <key-path>/script/
              Files.createDirectories(dstKeygenPath)
        .tapError:
          t => ZIO logError s"unable to create the script path: ${t.getMessage}"
      // copying
      _ <- ZIO
        .attempt:
          if Files.exists(dstKeygenPath) then Files.copy(srcKeygenPath, dstKeygenPath, REPLACE_EXISTING)
    yield ()

  def generateKeyPair(using w: Workspace, c: Configuration): Task[Path] =
    for
      // base configuration path
      path          <- ZIO attempt Paths.get(c.confPath)
      // specific workspace path for key pairs storing
      workspacePath <- ZIO attempt:
        path
          .resolve("keys")
          .resolve(w.id.stringValue)
      // key pairs generation script path
      keygenPath    <- ZIO attempt:
        path
          .resolve("script")
          .resolve("keygen.sh")

      _ <- ZIO
        .attempt:
          // <key-path>/keys/<workspace-id>/
          Files.createDirectories(workspacePath)
        .tapError:
          t => ZIO logError s"unable to create the key path for workspace ${w.id.stringValue}: ${t.getMessage}"

      _ <- ZIO
        .attempt:
          // creating private/public key pair by bash script
          s"${keygenPath.toString} -n ${w.id.stringValue} -p ${workspacePath.toString}".!

        .flatMap:
          case 0 => ZIO logInfo  s"keypair generated for workspace ${w.id.stringValue}"
          case _ => ZIO logError s"errors during keypair generation for workspace ${w.id.stringValue}"

    yield workspacePath
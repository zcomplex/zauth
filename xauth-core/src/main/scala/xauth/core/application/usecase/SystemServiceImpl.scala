package xauth.core.application.usecase

import xauth.core.common.model.AuthRole.{Admin as AdminR, System as SystemR, User as UserR}
import xauth.core.common.model.AuthStatus
import xauth.core.domain.client.port.ClientService
import xauth.core.domain.configuration.model.Configuration
import xauth.core.domain.system.model.SettingKey.Init
import xauth.core.domain.system.port.{SystemService, SystemSettingRepository}
import xauth.core.domain.tenant.port.TenantService
import xauth.core.domain.user.model.UserInfo
import xauth.core.domain.user.port.UserService
import xauth.core.domain.workspace.port.WorkspaceService
import zio.*

private final class SystemServiceImpl(
                                 settings: SystemSettingRepository,
                                 tenants: TenantService,
                                 workspaces: WorkspaceService,
                                 clients: ClientService,
                                 users: UserService,
                                 conf: Configuration
                               ) extends SystemService:

  private def isInit: Task[Boolean] =
    settings
      .read[Boolean](Init)
      .map(_.getOrElse(false))

  override def init: Task[Boolean] =
    isInit flatMap:
      // system already configured
      case true =>
        ZIO.logInfo("system is already configured") *> ZIO.succeed(false)

      // system is not configured
      case false =>

        val userInfo = UserInfo(
          firstName = conf.init.admin.info.firstName,
          lastName  = conf.init.admin.info.lastName,
          company   = conf.init.admin.info.company,
          contacts  = conf.init.admin.info.contacts
        )

        for {
          // creating root tenant
          _ <- tenants.createSystemTenant
          // creating root workspace
          w <- workspaces.createSystemWorkspace
          // configuring first client
          _ <- clients.create(conf.init.client.id, conf.init.client.secret)(using w)
          // configuring system admin user
          _ <- users.create(username = conf.init.admin.username,
                            password = conf.init.admin.password,
                         description = Some("system administrator"),
                            parentId = None,
                            userInfo = userInfo,
                              status = AuthStatus.Enabled,
                        applications = Nil,
                               roles = UserR, AdminR, SystemR)(using w)
          // saving new application state
          b <- settings.save(Init, true)
        } yield b

object SystemServiceImpl:

  val layer: ZLayer[SystemSettingRepository & TenantService & WorkspaceService & ClientService & UserService & Configuration, Nothing, SystemService] =
    ZLayer fromZIO:
      for
        settings   <- ZIO.service[SystemSettingRepository]
        tenants    <- ZIO.service[TenantService]
        workspaces <- ZIO.service[WorkspaceService]
        conf       <- ZIO.service[Configuration]
        clients    <- ZIO.service[ClientService]
        users      <- ZIO.service[UserService]
      yield new SystemServiceImpl(settings, tenants, workspaces, clients, users, conf)
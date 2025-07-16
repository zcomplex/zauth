package xauth.api

import xauth.api.info.InfoController
import xauth.api.model.info.Info
import xauth.core.application.usecase.*
import xauth.core.common.model.ContactType
import xauth.core.domain.configuration.model.{Configuration, InitConfiguration}
import xauth.core.domain.system.port.{SystemService, SystemSettingRepository}
import xauth.core.domain.user.model.{UserContact, UserInfo}
import xauth.core.domain.workspace.model.*
import xauth.generated.BuildInfo
import xauth.infrastructure.client.MongoClientRepository
import xauth.infrastructure.mongo.*
import xauth.infrastructure.setting.MongoSystemSettingRepository
import xauth.infrastructure.tenant.MongoTenantRepository
import xauth.infrastructure.user.MongoUserRepository
import xauth.infrastructure.workspace.MongoWorkspaceRepository
import xauth.util.Uuid
import zio.config.*
import zio.config.magnolia.*
import zio.config.typesafe.*
import zio.http.*
import zio.http.Method.GET
import zio.http.codec.*
import zio.http.endpoint.*
import zio.{Config, ConfigProvider, *}

import java.time.ZoneId

object Main extends ZIOAppDefault:

  private val infoEndpoint = Endpoint(GET / "info").out[Info]

  private val routes: Routes[Any, Nothing] = Routes(
    GET / Root -> handler(Response.text(s"X-Auth ${BuildInfo.Version}")),

    // get /info
    InfoController.GetInfo
  )

  def run: ZIO[Any, Throwable, Nothing] = {
    val effect = for
      cnf <- ZIO.service[Configuration]
      mdb <- ZIO.service[DefaultMongoClient]
      sys <- ZIO.service[SystemService]
      // Connecting to the system workspace
      _   <- mdb.connect(Uuid.Zero -> cnf.init.workspace.database.uri)
      // First initialization
      _   <- sys.init
      // Starting service
      _   <- Server.serve(routes)
    yield ()

    given Config[ZoneId]                 = Config.string mapAttempt ZoneId.of
    given Config[ContactType]            = Config.string mapAttempt ContactType.fromValue
    given Config[WorkspaceConfiguration] = deriveConfig[WorkspaceConfiguration]
    given Config[InitConfiguration]      = deriveConfig[InitConfiguration]
    given Config[Configuration]          = deriveConfig[Configuration]

    val configLayer: ZLayer[Any, Config.Error, Configuration] =
      ZLayer.fromZIO:
        val file = sys.props.get("config.file") getOrElse "conf/application.conf"
        ConfigProvider
          .fromHoconFilePath(file)
          .load(deriveConfig[Configuration])

    val mongoClient = DefaultDriver.layer >>> DefaultMongoClient.layer
    val systemSettingRepository = MongoSystemSettingRepository.layer

    val settings = MongoSystemSettingRepository.layer
    val tenants = MongoTenantRepository.layer >>> TenantServiceImpl.layer
    val workspaces = MongoWorkspaceRepository.layer >>> WorkspaceServiceImpl.layer
    val clients = MongoClientRepository.layer >>> ClientServiceImpl.layer
    val users = MongoUserRepository.layer >>> UserServiceImpl.layer

    val systemService = ZLayer.make[SystemService](
      configLayer, mongoClient, systemSettingRepository, tenants, workspaces, clients, users, SystemServiceImpl.layer
    )

    effect
      .as(ZIO.never)
      .flatten
      .provide(
        Server.default ++ configLayer ++ mongoClient ++ systemService
      )
  }
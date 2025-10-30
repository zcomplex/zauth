package xauth.api

import io.circe.*
import io.circe.config.parser.*
import io.circe.generic.auto.*
import xauth.api.auth.AuthController
import xauth.api.info.InfoController
import xauth.api.tenant.TenantController
import xauth.core.application.usecase.*
import xauth.core.common.model.ContactType
import xauth.core.domain.configuration.model.{Configuration as AppConf, *}
import xauth.core.domain.system.port.{SystemService, SystemSettingRepository}
import xauth.core.domain.user.model.{UserContact, UserInfo}
import xauth.core.domain.workspace.model.*
import xauth.core.domain.workspace.port.WorkspaceService
import xauth.core.spi.env.{TimeService, UuidService}
import xauth.generated.BuildInfo
import xauth.infrastructure.client.MongoClientRepository
import xauth.infrastructure.code.MongoAccountCodeRepository
import xauth.infrastructure.messaging.provider.ProviderRegistryImpl
import xauth.infrastructure.mongo.*
import xauth.infrastructure.setting.MongoSystemSettingRepository
import xauth.infrastructure.tenant.MongoTenantRepository
import xauth.infrastructure.user.MongoUserRepository
import xauth.infrastructure.workspace.MongoWorkspaceRepository
import zio.*
import zio.http.*
import zio.http.Method.GET

import java.util.Locale

object Main extends ZIOAppDefault:

  private val routes: Routes[Any, Nothing] = Routes(
    GET / Root -> handler(Response.text(s"X-Auth ${BuildInfo.Version}")),

    // get /info
    InfoController.GetInfo,

    // todo: get /health
    // todo: get /init/configure or /system/configure

    // /system/tenants
    TenantController.PostTenant,
    // todo: ...

    // Authentication
    // Basic authentication by trusted client
    // /auth
//    AuthController.PostToken
  )
  
//  val serverConfig = Server.Config.default
//    .port(1234)
//    .keepAlive(true)
//    .idleTimeout(30.seconds)
//    .requestDecompression(true)
//    .maxHeaderSize(8 * 1024)

  def run: ZIO[Any, Throwable, Nothing] = {
    val effect = for
      // First initialization
      sys <- ZIO.service[SystemService]
      _   <- sys.init
      // Starting service
      _   <- Server.serve(routes)
    yield ()

    given Decoder[Locale]      = Decoder.decodeString map Locale.forLanguageTag
    given Decoder[ContactType] = Decoder.decodeString map ContactType.fromValue

    val configLayer: ZLayer[Any, Throwable, AppConf] =
      ZLayer.fromZIO:
        for
          json   <- ZIO.succeed(os.read(os.pwd / "conf" / "application.conf"))
          config <- ZIO.fromEither(decode[AppConf](json))
        yield config

    val templateLayer = TemplateServiceImpl.layer(os.pwd / "conf" / "template")
    val messagingLayer = templateLayer >>> MessagingServiceImpl.layer

    val mongoClient = configLayer >>> DefaultDriver.layer >>> DefaultMongoClient.layer
    val environmentLayer = TimeService.layer ++ UuidService.layer >>> DefaultEnvironment.layer

    val systemSettingRepository = MongoSystemSettingRepository.layer
    val accountCodeRepository = MongoClientRepository.layer >>> MongoAccountCodeRepository.layer

    val settings = MongoSystemSettingRepository.layer
    val tenants = MongoTenantRepository.layer >>> TenantServiceImpl.layer
    val workspaces = MongoWorkspaceRepository.layer >>> WorkspaceServiceImpl.layer
    val clients = MongoClientRepository.layer >>> ClientServiceImpl.layer
    val codes = accountCodeRepository >>> AccountCodeServiceImpl.layer

    val users = MongoUserRepository.layer >>> UserServiceImpl.layer
    val workspaceRegistry = MongoWorkspaceRepository.layer >>> WorkspaceRegistry.layer

    val accountEventDispatcher = codes >>> AccountEventDispatcherImpl.layer

    val systemService = ZLayer.make[SystemService](
      configLayer, mongoClient, environmentLayer, accountEventDispatcher, ProviderRegistryImpl.layer,
      workspaceRegistry, messagingLayer, systemSettingRepository, tenants, workspaces, clients, users, SystemServiceImpl.layer
    )

    effect
      .as(ZIO.never)
      .flatten
      .provide(
        Server.default ++ configLayer ++ systemService
      )
  }
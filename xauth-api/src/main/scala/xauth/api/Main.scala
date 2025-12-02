package xauth.api

import io.circe.*
import io.circe.config.parser.*
import io.circe.generic.auto.*
import xauth.api.auth.AuthController
import xauth.api.info.InfoController
import xauth.api.tenant.TenantController
import xauth.core.application.usecase.*
import xauth.core.common.model.ContactType
import xauth.core.domain.code.port.AccountCodeRepository
import xauth.core.domain.configuration.model.{Configuration as AppConf, *}
import xauth.core.domain.system.port.{SystemService, SystemSettingRepository}
import xauth.core.domain.user.model.{UserContact, UserInfo}
import xauth.core.domain.workspace.model.*
import xauth.core.domain.workspace.port.WorkspaceRepository
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

  //  import scala.math.Fractional.Implicits.infixFractionalOps
  private val Home = GET / Root -> handler(Response.text(s"X-Auth ${BuildInfo.Version}"))


  private def routes(authC: AuthController) =
    (Routes.empty :+ Home)
      ++ InfoController.routes   // /info
      ++ authC.routes            // /auth    -> Authentication routes
      ++ TenantController.routes
  // /tenants
      // todo: get /health
      // todo: get /init/configure or /system/configure
      // todo: /system/tenants
  
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
      // Controller layers
      authC <- ZIO.service[AuthController]
      // Routes to serve
      allRoutes = routes(authC)
      // Starting service
      _  <- Server.serve(allRoutes)
    yield ()

    given Decoder[Locale]      = Decoder.decodeString map Locale.forLanguageTag
    given Decoder[ContactType] = Decoder.decodeString map ContactType.fromValue

    val config: ZLayer[Any, Throwable, AppConf] =
      ZLayer.fromZIO:
        for
          json   <- ZIO.succeed(os.read(os.pwd / "conf" / "application.conf"))
          config <- ZIO.fromEither(decode[AppConf](json))
        yield config

    val templateLayer = TemplateServiceImpl.layer(os.pwd / "conf" / "template")

    val messaging = templateLayer
      >>> MessagingServiceImpl.layer

    val database = config
      >>> DefaultDriver.layer
      >>> DefaultMongoClient.layer

    val environment = TimeService.layer ++ UuidService.layer
      >>> DefaultEnvironment.layer

    val systemSettingRepository = MongoSystemSettingRepository.layer
    val accountCodeRepository = MongoAccountCodeRepository.layer

    val settings = config
      >>> database
      >>> MongoSystemSettingRepository.layer

    val tenants = config
      >>> database
      >>> MongoTenantRepository.layer
      >>> TenantServiceImpl.layer

    val workspaces = config
      >>> database
      >>> MongoWorkspaceRepository.layer
      >>> WorkspaceServiceImpl.layer

    val clients = config
      >>> database
      >>> MongoClientRepository.layer
      >>> ClientServiceImpl.layer

    val codes = MongoAccountCodeRepository.layer
      >>> AccountCodeServiceImpl.layer

    val users = config
      >>> database
      >>> MongoUserRepository.layer
      >>> UserServiceImpl.layer

    val workspaceRegistry = config
      >>> database
      >>> MongoWorkspaceRepository.layer
      >>> WorkspaceRegistry.layer

    val accountEventDispatcher = config
      >>> database
      >>> codes
      >>> AccountEventDispatcherImpl.layer

    val systemService = ZLayer.make[SystemService](
      config, database, environment, accountEventDispatcher, ProviderRegistryImpl.layer,
      workspaceRegistry, messaging, systemSettingRepository, tenants, workspaces, clients, users,
      SystemServiceImpl.layer
    )

    val controllersLayer = AuthController.layer

    // todo: handle graceful shutdown
    // todo: handle idle timeout

    effect
      .as(ZIO.never)
      .flatten
      .provide(
        Server.default ++ systemService ++ controllersLayer
      )
  }
package xauth.api

import io.circe.*
import io.circe.config.parser.*
import io.circe.generic.auto.*
import xauth.api.auth.AuthController
import xauth.api.info.InfoController
import xauth.api.tenant.TenantController
import xauth.core.application.usecase.*
import xauth.core.common.model.ContactType
import xauth.core.domain.client.port.{ClientRepository, ClientService}
import xauth.core.domain.code.port.{AccountCodeRepository, AccountCodeService}
import xauth.core.domain.configuration.model.{Configuration as AppConf, *}
import xauth.core.domain.system.port.{SystemService, SystemSettingRepository}
import xauth.core.domain.tenant.port.{TenantRepository, TenantService}
import xauth.core.domain.user.model.{UserContact, UserInfo}
import xauth.core.domain.user.port.{UserRepository, UserService}
import xauth.core.domain.workspace.model.*
import xauth.core.domain.workspace.port.{WorkspaceRepository, WorkspaceService}
import xauth.core.spi.MessagingProvider.ProviderRegistry
import xauth.core.spi.env.{TimeService, UuidService}
import xauth.core.spi.{AccountEventDispatcher, MessagingService, TemplateService, env}
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

    object layer:
      val config: TaskLayer[AppConf] =
        ZLayer.fromZIO:
          for
            json   <- ZIO.succeed(os.read(os.pwd / "conf" / "application.conf"))
            config <- ZIO.fromEither(decode[AppConf](json))
          yield config

      val environment: ULayer[env.Environment] = TimeService.layer ++ UuidService.layer
        >>> DefaultEnvironment.layer

      object service:
        val accountCode: URLayer[env.Environment & AccountCodeRepository, AccountCodeService] =
          AccountCodeServiceImpl.layer

        val client: URLayer[ClientRepository, ClientService] =
          ClientServiceImpl.layer

        val template: TaskLayer[TemplateService] =
          TemplateServiceImpl.layer(os.pwd / "conf" / "template")

        val messaging: RLayer[TemplateService & WorkspaceRegistry, MessagingService] = template
          >>> MessagingServiceImpl.layer

        val tenant: URLayer[TenantRepository, TenantService] =
          TenantServiceImpl.layer

        val user: URLayer[UserRepository & AccountEventDispatcher, UserServiceImpl] =
          UserServiceImpl.layer

        val workspace: URLayer[WorkspaceRegistry & WorkspaceRepository & AppConf, WorkspaceService] =
          WorkspaceServiceImpl.layer

        val system: URLayer[SystemSettingRepository & TenantService & WorkspaceService & ClientService & UserService & AppConf, SystemService] =
          SystemServiceImpl.layer

      object persistence:
        object mongodb: // todo: use abstract types on +ROut
          val client: RLayer[AppConf, DefaultMongoClient] = config
            >>> DefaultDriver.layer
            >>> DefaultMongoClient.layer

          object repository: // todo: use abstract types on +ROut
            val accountCode: URLayer[DefaultMongoClient, AccountCodeRepository] =
              MongoAccountCodeRepository.layer

            val tenant: URLayer[DefaultMongoClient, MongoTenantRepository] =
              MongoTenantRepository.layer

            val workspace: URLayer[DefaultMongoClient, WorkspaceRepository] =
              MongoWorkspaceRepository.layer

            val client: URLayer[DefaultMongoClient, MongoClientRepository] =
              MongoClientRepository.layer

            val systemSetting: URLayer[DefaultMongoClient, SystemSettingRepository] =
              MongoSystemSettingRepository.layer

            val user: URLayer[DefaultMongoClient, MongoUserRepository] =
              MongoUserRepository.layer

      val workspaceRegistry: RLayer[ProviderRegistry & WorkspaceRepository, WorkspaceRegistry] =
        WorkspaceRegistry.layer

      val accountEventDispatcher: URLayer[AccountCodeService & MessagingService, AccountEventDispatcher] =
        AccountEventDispatcherImpl.layer

      val providerRegistry: ULayer[ProviderRegistry] =
        ProviderRegistryImpl.layer

      object controller:
        val auth: ULayer[AuthController] = AuthController.layer

    // todo: handle graceful shutdown
    // todo: handle idle timeout

    effect
      .as(ZIO.never)
      .flatten
      .provide(
        Server.default,

        layer.accountEventDispatcher,
        layer.config,
        layer.environment,
        layer.providerRegistry,
        layer.workspaceRegistry,

        layer.persistence.mongodb.client,
        layer.persistence.mongodb.repository.accountCode,
        layer.persistence.mongodb.repository.client,
        layer.persistence.mongodb.repository.systemSetting,
        layer.persistence.mongodb.repository.tenant,
        layer.persistence.mongodb.repository.user,
        layer.persistence.mongodb.repository.workspace,

        layer.service.accountCode,
        layer.service.client,
        layer.service.messaging,
        layer.service.system,
        layer.service.template,
        layer.service.tenant,
        layer.service.user,
        layer.service.workspace,

        // controllers
        layer.controller.auth
      )
  }
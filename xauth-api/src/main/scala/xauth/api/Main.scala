package xauth.api

import io.circe.*
import io.circe.config.parser.*
import io.circe.generic.semiauto.*
import xauth.api.info.InfoController
import xauth.api.model.info.Info
import xauth.core.application.usecase.*
import xauth.core.common.model.ContactType
import xauth.core.domain.configuration.model.{ClientConfiguration, Configuration, InitConfiguration, UserConfiguration}
import xauth.core.domain.system.port.{SystemService, SystemSettingRepository}
import xauth.core.domain.user.model.{UserContact, UserInfo}
import xauth.core.domain.workspace.model.*
import xauth.generated.BuildInfo
import xauth.infrastructure.client.MongoClientRepository
import xauth.infrastructure.messaging.provider.ProviderRegistryImpl
import xauth.infrastructure.mongo.*
import xauth.infrastructure.setting.MongoSystemSettingRepository
import xauth.infrastructure.tenant.MongoTenantRepository
import xauth.infrastructure.user.MongoUserRepository
import xauth.infrastructure.workspace.MongoWorkspaceRepository
import zio.*
import zio.http.*
import zio.http.Method.GET
import zio.http.codec.*
import zio.http.endpoint.*

import java.nio.file.{Files, Paths}

object Main extends ZIOAppDefault:

  private val infoEndpoint = Endpoint(GET / "info").out[Info]

  private val routes: Routes[Any, Nothing] = Routes(
    GET / Root -> handler(Response.text(s"X-Auth ${BuildInfo.Version}")),

    // get /info
    InfoController.GetInfo
  )

  def run: ZIO[Any, Throwable, Nothing] = {
    val effect = for
      // First initialization
      sys <- ZIO.service[SystemService]
      _   <- sys.init
      // Starting service
      _   <- Server.serve(routes)
    yield ()

    given Decoder[Encryption]            = deriveDecoder
    given Decoder[Expiration]            = deriveDecoder
    given Decoder[Jwt]                   = deriveDecoder
    given Decoder[ProviderConf]          = deriveDecoder
    given Decoder[MessagingConf]         = deriveDecoder
    given Decoder[RoutesConfiguration]   = deriveDecoder
    given Decoder[FrontEndConfiguration] = deriveDecoder
    given Decoder[DatabaseConf]          = deriveDecoder
    given Decoder[WorkspaceConfiguration]= deriveDecoder
    given Decoder[ClientConfiguration]   = deriveDecoder
    given Decoder[ContactType]           = Decoder.decodeString map ContactType.fromValue
    given Decoder[UserContact]           = deriveDecoder
    given Decoder[UserInfo]              = deriveDecoder
    given Decoder[UserConfiguration]     = deriveDecoder
    given Decoder[InitConfiguration]     = deriveDecoder
    given Decoder[Configuration]         = deriveDecoder

    val configLayer: ZLayer[Any, Throwable, Configuration] =
      ZLayer.fromZIO:
        val file = sys.props.get("config.file") getOrElse "conf/application.conf"
        for
          bytes  <- ZIO.attempt(Files.readAllBytes(Paths.get(file)))
          json   <- ZIO.succeed(new String(bytes, "UTF-8"))
          config <- ZIO.fromEither(decode[Configuration](json))
        yield config

    val messagingLayer = MessagingServiceImpl.layer

    val mongoClient = configLayer >>> DefaultDriver.layer >>> DefaultMongoClient.layer
    val systemSettingRepository = MongoSystemSettingRepository.layer

    val settings = MongoSystemSettingRepository.layer
    val tenants = MongoTenantRepository.layer >>> TenantServiceImpl.layer
    val workspaces = MongoWorkspaceRepository.layer >>> WorkspaceServiceImpl.layer
    val clients = MongoClientRepository.layer >>> ClientServiceImpl.layer
    val users = MongoUserRepository.layer >>> UserServiceImpl.layer
    
    val workspaceRegistry =  MongoWorkspaceRepository.layer >>> WorkspaceRegistry.layer

    val systemService = ZLayer.make[SystemService](
      configLayer, mongoClient, ProviderRegistryImpl.layer, workspaceRegistry, messagingLayer, systemSettingRepository, tenants, workspaces, clients, users, SystemServiceImpl.layer
    )

    effect
      .as(ZIO.never)
      .flatten
      .provide(
        Server.default ++ configLayer ++ systemService
      )
  }
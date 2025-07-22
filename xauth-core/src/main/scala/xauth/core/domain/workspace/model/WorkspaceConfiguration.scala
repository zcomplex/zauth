package xauth.core.domain.workspace.model

import io.circe.Json
import xauth.core.domain.workspace.model.ProviderConf.PConf

import java.time.ZoneId

case class Expiration(accessToken: Int, refreshToken: Int)

case class Encryption(algorithm: String)

case class Jwt(expiration: Expiration, encryption: Encryption)

case class RoutesConfiguration
(
  activation: String,
  deletion: String,
  contactTrust: String,
  passwordReset: String,
  registrationInvitation: String
)

case class FrontEndConfiguration(baseUrl: String, routes: RoutesConfiguration)

case class SmtpConfiguration(host: String, port: Int, user: String, pass: String, channel: String, debug: Boolean)
case class MailConfiguration(from: String, name: String, smtp: SmtpConfiguration)

case class WorkspaceConfiguration(database: DatabaseConf, frontEnd: FrontEndConfiguration, messaging: MessagingConf, jwt: Jwt, applications: Seq[String], zoneId: ZoneId)

case class DatabaseConf(uri: String)

case class ProviderConf(name: String, active: Boolean, configuration: PConf)
case class MessagingConf(enabled: Boolean, providers: Seq[ProviderConf])

object ProviderConf:
  type PConf = Map[String, Json]

  def empty(name: String = ""): ProviderConf =
    ProviderConf(name, active = true, Map.empty)
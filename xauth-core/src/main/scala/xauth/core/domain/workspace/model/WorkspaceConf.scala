package xauth.core.domain.workspace.model

import io.circe.Json
import xauth.core.domain.workspace.model.ProviderConf.PConf

import java.time.ZoneId
import java.util.Locale

case class Expiration(accessToken: Int, refreshToken: Int)

case class Encryption(algorithm: String)

case class Jwt(expiration: Expiration, encryption: Encryption)

case class RoutesConf
(
  activation: String,
  deletion: String,
  contactTrust: String,
  passwordReset: String,
  registrationInvitation: String
)

case class FrontEndConf(baseUrl: String, routes: RoutesConf)

case class SmtpConf(host: String, port: Int, user: String, pass: String, channel: String, debug: Boolean)
case class MailConf(from: String, name: String, smtp: SmtpConf)

case class WorkspaceConf
(
  locale: Locale,
  timezone: ZoneId,
  database: DatabaseConf,
  jwt: Jwt,
  messaging: MessagingConf,
  applications: Seq[String],
  frontEnd: FrontEndConf
)

case class DatabaseConf(uri: String)

case class ProviderConf(name: String, active: Boolean, configuration: PConf)
case class MessagingConf(enabled: Boolean, providers: Seq[ProviderConf])

object ProviderConf:
  type PConf = Map[String, Json]

  def empty(name: String = ""): ProviderConf =
    ProviderConf(name, active = true, Map.empty)
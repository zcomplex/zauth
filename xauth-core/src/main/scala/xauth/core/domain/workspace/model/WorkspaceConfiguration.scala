package xauth.core.domain.workspace.model

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

case class WorkspaceConfiguration(database: DatabaseConf, frontEnd: FrontEndConfiguration, mail: MailConfiguration, jwt: Jwt, applications: Seq[String], zoneId: ZoneId)

case class DatabaseConf(uri: String)
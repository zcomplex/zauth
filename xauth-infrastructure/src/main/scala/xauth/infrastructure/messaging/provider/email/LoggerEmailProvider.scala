package xauth.infrastructure.messaging.provider.email

import xauth.core.domain.workspace.model.ProviderConf
import xauth.core.spi.EmailProvider
import xauth.infrastructure.messaging.provider.email.LoggerEmailProvider.Name
import zio.{UIO, ZIO}

final class LoggerEmailProvider extends EmailProvider:

  override val conf: ProviderConf = ProviderConf.empty(Name)

  override def sendText(to: String, subject: String, message: String): UIO[Unit] =
    ZIO logInfo s"logger: sending email to $to"

object LoggerEmailProvider:
  val Name = "email.logger"
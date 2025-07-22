package xauth.infrastructure.messaging.provider.sms

import xauth.core.domain.workspace.model.ProviderConf
import xauth.core.spi.SmsProvider
import xauth.infrastructure.messaging.provider.sms.LoggerSmsProvider.Name
import zio.{UIO, ZIO}

final class LoggerSmsProvider extends SmsProvider:

  override val conf: ProviderConf = ProviderConf.empty(Name)

  override def send(to: String, message: String): UIO[Unit] =
    ZIO logInfo s"logger: sending sms to $to"

object LoggerSmsProvider:
  val Name = "sms.logger"
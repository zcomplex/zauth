package xauth.infrastructure.messaging.provider

import xauth.core.domain.workspace.model.Workspace
import xauth.core.spi.MessagingProvider
import xauth.core.spi.MessagingProvider.ProviderRegistry
import xauth.infrastructure.messaging.provider.email.{LoggerEmailProvider, QueuedEmailProvider}
import xauth.infrastructure.messaging.provider.sms.LoggerSmsProvider
import zio.*

private class ProviderRegistryImpl extends ProviderRegistry:

  override def providers(using w: Workspace): UIO[Set[MessagingProvider]] =
    ZIO
      .foreach(w.configuration.messaging.providers.filter(_.active)): p =>
        p.name match
          case QueuedEmailProvider.Name =>
            QueuedEmailProvider.make(p) map Some.apply
          case LoggerEmailProvider.Name =>
            ZIO.succeed(Some(new LoggerEmailProvider))
          case LoggerSmsProvider.Name =>
            ZIO.succeed(Some(new LoggerSmsProvider))
          case _ => ZIO.none
      .map(_.flatten.toSet)

object ProviderRegistryImpl:
  val layer: ZLayer[Any, Nothing, ProviderRegistry] =
    ZLayer.succeed(new ProviderRegistryImpl)
package xauth.core.application.usecase

import xauth.core.domain.user.model.User
import xauth.core.domain.workspace.model.Workspace
import xauth.core.spi.{EmailProvider, MessagingService, SmsProvider}
import zio.{UIO, URLayer, ZIO, ZLayer}

private final class MessagingServiceImpl(registry: WorkspaceRegistry) extends MessagingService:

  // todo: add template system for messages
  // todo: develop auto-updating cache for all email user listeners

  /** Notifies user registration to the listeners. */
  override def notifyUserRegistration(u: User)(using w: Workspace): UIO[Unit] =
    ZIO logInfo s"mock: dispatching new-user notifications"

  /** Sends activation message with the activation instructions. */
  override def sendActivationMessage(u: User)(using w: Workspace): UIO[Unit] =
    for
      ep <- registry.messaging[EmailProvider]
      sm <- registry.messaging[SmsProvider]

      _  <- ZIO.when(ep.isEmpty && sm.isEmpty):
        ZIO logWarning s"no email/sms provider found"

      _ <-
        // email provider and untrusted email available
        val s1 = ep zip u.email(trusted = false) map:
          (p, e) => p.sendText(e, "<subject>", "<body>")
        // sms provider and untrusted mobile number available
        val s2 = sm zip u.mobileNumber(trusted = false) map:
          (p, n) => p.send(n, "<message>")
        // email priority
        (s1 orElse s2) getOrElse ZIO.logWarning(s"skipped sending activation message for user ${u.username}")
    yield ()

object MessagingServiceImpl:
  val layer: URLayer[WorkspaceRegistry, MessagingService] =
    ZLayer.fromZIO:
      for
        r <- ZIO.service[WorkspaceRegistry]
      yield new MessagingServiceImpl(r)
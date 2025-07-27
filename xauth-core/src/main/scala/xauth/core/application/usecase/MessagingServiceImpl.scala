package xauth.core.application.usecase

import xauth.core.domain.code.model.AccountCode
import xauth.core.domain.user.model.User
import xauth.core.domain.workspace.model.Workspace
import xauth.core.spi.TemplateService.AccountActD
import xauth.core.spi.{EmailProvider, MessagingService, SmsProvider, TemplateService}
import zio.{UIO, URLayer, ZIO, ZLayer}

import java.time.format.DateTimeFormatter
import java.util.Locale

private final class MessagingServiceImpl(registry: WorkspaceRegistry, templates: TemplateService) extends MessagingService:

  // todo: add template system for messages
  // todo: develop auto-updating cache for all email user listeners
  
  private def formatter(l: Locale) = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy HH:mm z", l)

  /** Notifies user registration to the listeners. */
  override def notifyUserRegistration(u: User)(using w: Workspace): UIO[Unit] =
    ZIO logInfo s"mock: dispatching new-user notifications"

  /** Sends activation message with the activation instructions. */
  override def sendActivationMessage(u: User, c: AccountCode)(using w: Workspace): UIO[Unit] =
    for
      ep <- registry.messaging[EmailProvider]
      sm <- registry.messaging[SmsProvider]

      _  <- ZIO.when(ep.isEmpty && sm.isEmpty):
        ZIO logWarning s"no email/sms provider found"

      _ <-
        // todo: handle effects
        val zdt = c.expiresAt.atZone(w.configuration.timezone)
        
        val data: AccountActD = (
          code = c.code,
          codeExp = formatter(w.configuration.locale).format(zdt),
          link = w.configuration.frontEnd.baseUrl + w.configuration.frontEnd.routes.activation
        )
        // email provider and untrusted email available
        val s1 = ep zip u.email(trusted = false) map:
          (p, e) => templates
            .accountActivationEmail(u, data)
            .flatMap:
              t => p.sendText(e, t.subject, t.message)
            .catchAll:
              e => ZIO logError s"unable to obtain 'account-activation' email template: ${e.getMessage}"
        // sms provider and untrusted mobile number available
        val s2 = sm zip u.mobileNumber(trusted = false) map:
          (p, n) => templates
            .accountActivationSms(u, data)
            .flatMap:
              t => p.send(n, t.message)
            .catchAll:
              e => ZIO logError s"unable to obtain 'account-activation' sms template: ${e.getMessage}"
        // email priority
        (s1 orElse s2) getOrElse ZIO.logWarning(s"skipped sending activation message for user ${u.username}")
    yield ()

object MessagingServiceImpl:
  val layer: URLayer[WorkspaceRegistry & TemplateService, MessagingService] =
    ZLayer.fromZIO:
      for
        r <- ZIO.service[WorkspaceRegistry]
        t <- ZIO.service[TemplateService]
      yield new MessagingServiceImpl(r, t)
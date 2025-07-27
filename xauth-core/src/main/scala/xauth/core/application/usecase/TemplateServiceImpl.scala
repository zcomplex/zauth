package xauth.core.application.usecase

import os.Path
import xauth.core.domain.user.model.User
import xauth.core.domain.workspace.model.Workspace
import xauth.core.spi.TemplateService
import xauth.core.spi.TemplateService.*
import zio.{Task, ZIO, ZLayer}

import scala.reflect.ClassTag

class TemplateServiceImpl(templates: Map[String, Template]) extends TemplateService:

  extension (ts: Map[String, Template])
    private def get[A <: Template](t: String, lang: String, name: String): Task[A] =
      val key = s"$t.$lang.$name"
      ZIO
        .fromOption:
          ts.get(key).map(_.asInstanceOf[A])
        .orElseFail:
          TemplateNotFound(key)

    private infix def email(name: String)(using w: Workspace): Task[EmailT] =
      get[EmailT]("email", w.configuration.locale.getLanguage, name)

    private infix def sms(name: String)(using w: Workspace): Task[SmsT] =
      get[SmsT]("sms", w.configuration.locale.getLanguage, name)

  private case class TemplateNotFound(name: String) extends Throwable

  override def accountActivationEmail(u: User, d: AccountActD)(using w: Workspace): Task[EmailT] =
    templates.email("account-activation") map: e =>
      e.copy(subject = e.subject.filled(u, d), message = e.message.filled(u, d))

  override def accountActivationSms(u: User, d: AccountActD)(using w: Workspace): Task[SmsT] =
    templates.sms("account-activation") map: e =>
      e.copy(message = e.message.filled(u, d))

  override def accountDeletionEmail(u: User)(using w: Workspace): Task[EmailT] = ???

  override def accountDeletionSms(u: User)(using w: Workspace): Task[SmsT] = ???

  override def contactTrustEmail(u: User)(using w: Workspace): Task[EmailT] = ???

  override def contactTrustSms(u: User)(using w: Workspace): Task[SmsT] = ???

  override def passwordResetEmail(u: User)(using w: Workspace): Task[EmailT] = ???

  override def passwordResetSms(u: User)(using w: Workspace): Task[SmsT] = ???

  override def registrationInvitationEmail(u: User)(using w: Workspace): Task[EmailT] = ???

  override def registrationInvitationSms(u: User)(using w: Workspace): Task[SmsT] = ???

object TemplateServiceImpl:

  def layer(p: Path, filter: Path => Boolean = _ => true): ZLayer[Any, Throwable, TemplateServiceImpl] =
    ZLayer.fromZIO:
      read[Template](p, filter) map:
        new TemplateServiceImpl(_)

  import io.circe.*
  import io.circe.config.parser.*
  import io.circe.generic.extras.*
  import io.circe.generic.extras.semiauto.*

  private given Configuration = Configuration(
    transformMemberNames = identity,
    transformConstructorNames = _.toLowerCase.init,
    useDefaults = false,
    discriminator = Some("type"),
    strictDecoding = false
  )

  private given Decoder[SmsT] = deriveConfiguredDecoder
  private given Decoder[EmailT] = deriveConfiguredDecoder
  private given Decoder[Template] = deriveConfiguredDecoder

  private def read[A <: Template](p: Path, filter: Path => Boolean = _ => true)(using ct: ClassTag[A], d: Decoder[A]) =
    for
      map <- ZIO.attempt:
        os
          .list(p)
          .filter(os.isFile)
          .filter(filter)
          .flatMap: path =>
            val content = os.read(path)
            decode[A](content) match
              case Left(e) => None
              case Right(v) => Some(path.last.replaceAll(".conf$", "") -> v)
          .toMap
    yield map
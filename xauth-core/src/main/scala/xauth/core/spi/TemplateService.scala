package xauth.core.spi

import xauth.core.domain.user.model.User
import xauth.core.domain.workspace.model.Workspace
import zio.Task

trait TemplateService:

  import TemplateService.*
  
  extension (s: String)
    protected def filled(u: User, d: AccountActD)(using w: Workspace): String = s
      .replace("firstName", u.info.firstName)
      .replace("code", d.code)
      .replace("codeExpiration", d.codeExp)
      .replace("link", d.link)
      .replace("company", w.company.name)
      .replace("companyDescription", w.company.description)

  def accountActivationEmail(u: User, d: AccountActD)(using w: Workspace): Task[EmailT]
  def accountActivationSms(u: User, d: AccountActD)(using w: Workspace): Task[SmsT]

  def accountDeletionEmail(u: User)(using w: Workspace): Task[EmailT]
  def accountDeletionSms(u: User)(using w: Workspace): Task[SmsT]

  def contactTrustEmail(u: User)(using w: Workspace): Task[EmailT]
  def contactTrustSms(u: User)(using w: Workspace): Task[SmsT]

  def passwordResetEmail(u: User)(using w: Workspace): Task[EmailT]
  def passwordResetSms(u: User)(using w: Workspace): Task[SmsT]

  def registrationInvitationEmail(u: User)(using w: Workspace): Task[EmailT]
  def registrationInvitationSms(u: User)(using w: Workspace): Task[SmsT]
  
object TemplateService:

  type AccountActD = (code: String, codeExp: String, link: String)

  sealed trait Template
  final case class EmailT(subject: String, message: String) extends Template
  final case class SmsT(message: String) extends Template
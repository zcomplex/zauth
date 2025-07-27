package xauth.core.domain.code.port

import xauth.core.domain.code.model.{AccountCode, AccountCodeType, UserContactData}
import xauth.core.domain.workspace.model.Workspace
import xauth.util.Uuid
import zio.Task

import java.time.Instant
import java.time.temporal.TemporalAmount

trait AccountCodeService:

  protected def generateCode: String

//  def find(code: String)(using w: Workspace): Task[Option[AccountCode]]

//  def find(code: String, kind: AccountCodeType)(using w: Workspace): Task[Option[AccountCode]]

//  def find(referenceId: Uuid, kind: AccountCodeType)(using w: Workspace): Task[Option[AccountCode]]

//  def delete(code: String)(using w: Workspace): Task[Unit]

//  def deleteAllExpired(using w: Workspace): Task[Unit]

  def create(kind: AccountCodeType, refId: Uuid, data: Option[UserContactData])(using w: Workspace): Task[AccountCode]

  def create(code: String, kind: AccountCodeType, refId: Uuid, data: Option[UserContactData], validity: Either[TemporalAmount, Instant])(using w: Workspace): Task[AccountCode]

package xauth.core.application.usecase

import xauth.core.domain.code.model.{AccountCode, AccountCodeType, UserContactData}
import xauth.core.domain.code.port.{AccountCodeRepository, AccountCodeService}
import xauth.core.domain.workspace.model.Workspace
import xauth.core.spi.env.Environment
import xauth.util.Uuid
import xauth.util.ext.random
import zio.{Task, ZIO, ZLayer}

import java.time.Duration.ofHours
import java.time.Instant
import java.time.temporal.TemporalAmount

private final class AccountCodeServiceImpl(env: Environment, repository: AccountCodeRepository) extends AccountCodeService:

  override protected def generateCode: String =
    (('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9')) random 32

  override def create(kind: AccountCodeType, refId: Uuid, data: Option[UserContactData])(using w: Workspace): Task[AccountCode] =
    create(generateCode, kind, refId, data, Left(ofHours(1)))

  override def create(code: String, kind: AccountCodeType, refId: Uuid, data: Option[UserContactData], validity: Either[TemporalAmount, Instant])(using w: Workspace): Task[AccountCode] =
    for
      now <- env.time.now

      expiresAt <- ZIO.attempt:
        validity match
          case Left(a) => now.plus(a)
          case Right(d) => d

      c <- repository save AccountCode(
        code      = code,
        kind      = kind,
        refId     = refId,
        data      = data,
        expiresAt = expiresAt,
        createdBy = Uuid.Zero,
        createdAt = now,
        updatedBy = Uuid.Zero,
        updatedAt = now
      )
    yield c

object AccountCodeServiceImpl:
  
  val layer: ZLayer[Environment & AccountCodeRepository, Nothing, AccountCodeService] =
    ZLayer fromZIO:
      for
        e <- ZIO.service[Environment]
        r <- ZIO.service[AccountCodeRepository]
      yield new AccountCodeServiceImpl(e, r)
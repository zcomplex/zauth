package xauth.core.application.usecase

import xauth.core.spi.env.{Environment, TimeService, UuidService}
import zio.ZLayer

private final case class DefaultEnvironment(time: TimeService, uuid: UuidService) extends Environment

object DefaultEnvironment:

  val layer: ZLayer[TimeService & UuidService, Nothing, Environment] =
    ZLayer fromFunction DefaultEnvironment.apply

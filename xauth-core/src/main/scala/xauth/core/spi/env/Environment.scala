package xauth.core.spi.env

trait Environment:
  val time: TimeService
  val uuid: UuidService
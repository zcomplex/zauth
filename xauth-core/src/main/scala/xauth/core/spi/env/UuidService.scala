package xauth.core.spi.env

import xauth.util.Uuid
import zio.{UIO, ULayer, ZIO, ZLayer}

trait UuidService:
  def generateUuid: UIO[Uuid]

object UuidService extends UuidService:

  override def generateUuid: UIO[Uuid] = ZIO succeed Uuid()

  val layer: ULayer[UuidService.type] = ZLayer succeed this
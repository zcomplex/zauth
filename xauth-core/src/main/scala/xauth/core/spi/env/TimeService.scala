package xauth.core.spi.env

import xauth.core.domain.workspace.model.Workspace
import zio.{UIO, ULayer, ZIO, ZLayer}

import java.time.{Instant, ZonedDateTime}

trait TimeService:
  def now: UIO[Instant]
  def nowAt(using w: Workspace): UIO[ZonedDateTime]
//  def toLocal(instant: Instant, workspace: Workspace): UIO[ZonedDateTime]
//  def format(zdt: ZonedDateTime, locale: Locale): UIO[String]

object TimeService extends TimeService:
  def now: UIO[Instant] = ZIO succeed Instant.now

  def nowAt(using w: Workspace): UIO[ZonedDateTime] =
    ZIO succeed ZonedDateTime.now(w.configuration.timezone)

  val layer: ULayer[TimeService.type] = ZLayer succeed this

//  def toLocal(i: Instant)(using w: Workspace): UIO[ZonedDateTime] =
//    ZIO succeed ZonedDateTime.ofInstant(i, w.configuration.timezone)
//
//  def format(d: ZonedDateTime, l: Locale): UIO[String] =
//    ZIO succeed d.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy HH:mm:ss z", l))
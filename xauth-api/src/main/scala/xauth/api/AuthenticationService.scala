package xauth.api

import xauth.core.domain.user.model.User
import zio.{IO, ULayer, ZIO, ZLayer}
import zio.http.{Header, Request}

trait AuthenticationService:
  def authenticate(r: Request): IO[String, User]

object AuthenticationService:
  val layer: ULayer[AuthenticationService] = ???/*ZLayer.succeed:
    new AuthenticationService:
      override def authenticate(r: Request): IO[String, User] =
        r.headers(Header.Authorization) match
          case Some(Header.Authorization.Bearer(token)) => ???
          case _ => ZIO.fail("unauthorized")*/
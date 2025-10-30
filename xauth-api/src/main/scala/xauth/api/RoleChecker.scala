package xauth.api

import xauth.api.Main.validateEnv
import xauth.api.jwt.JwtHelper
import xauth.core.application.usecase.WorkspaceRegistry
import xauth.core.common.model.AuthRole
import xauth.core.domain.user.port.UserService
import zio.ZIO
import zio.http.{Handler, HandlerAspect, Request, Response}

object RoleChecker:

  def roleAspect(roles: AuthRole*): HandlerAspect[WorkspaceRegistry & JwtHelper & UserService & UserContext, UserContext] =
    ???
//    HandlerAspect.interceptIncomingHandler[WorkspaceRegistry & JwtHelper & UserService, UserContext]:
//      Handler.fromFunctionZIO[Request]: request =>
//        for
//          context <- ZIO.service[UserContext]
//          _ <- ZIO
//            .fail(Response.forbidden(s"access restricted to: ${roles.mkString(", ")}"))
//            .unless(roles.exists(context.user.roles.contains))
//        yield (request, context)

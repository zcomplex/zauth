package xauth.api

import xauth.api.jwt.JwtHelper
import xauth.core.application.usecase.WorkspaceRegistry
import xauth.core.common.model.AuthRole
import xauth.core.domain.user.port.UserService
import zio.ZIO
import zio.http.{Handler, Request, Response}

object RoleChecker:
  
  type Env = WorkspaceRegistry & JwtHelper & UserService & UserContext
  type CxtOut = RoleContext

  private type CxtIn = UserResolver.CxtOut

  private type RoleHandler = Handler[Env, Response, (CxtIn, Request), (CxtOut, Request)]

  def withRoles(rs: AuthRole*): RoleHandler =
    Handler.fromFunctionZIO[(CxtIn, Request)]:
      case (u, r) =>
        val roles = rs intersect u.user.roles
        if roles.isEmpty then
          ZIO fail Response.forbidden(s"access restricted to: ${rs.mkString(", ")}")
        else
          ZIO succeed (RoleContext(u.workspace, u.user, roles), r)
package xauth.api

import xauth.api.Main.validateEnv
import xauth.api.jwt.JwtHelper
import xauth.core.application.usecase.WorkspaceRegistry
import xauth.core.common.model.AuthRole
import xauth.core.domain.client.model.Client
import xauth.core.domain.client.port.ClientService
import xauth.core.domain.user.model.User
import xauth.core.domain.user.port.UserService
import xauth.core.domain.workspace.model.Workspace
import zio.ZIO
import zio.http.*

final case class ClientCredentials(id: String, secret: String)

class WorkspaceContext(val workspace: Workspace)
class ClientContext(override val workspace: Workspace, val client: Client) extends WorkspaceContext(workspace)
class UserContext(override val workspace: Workspace, val user: User) extends WorkspaceContext(workspace)

class AuthenticationManager:

  val WorkspaceAspect: HandlerAspect[WorkspaceRegistry, WorkspaceContext] =
    WorkspaceResolver.aspect

  val ClientAspect: HandlerAspect[WorkspaceRegistry & ClientService, (WorkspaceContext, ClientContext)] =
//    WorkspaceAspect ++ ClientResolver.aspect
    ???

  val UserAspect: HandlerAspect[WorkspaceRegistry & JwtHelper & UserService, (WorkspaceContext, UserContext)] =
//    WorkspaceAspect ++ UserResolver.aspect
    ???
  
  def RoleAspect(roles: AuthRole*): HandlerAspect[WorkspaceRegistry & JwtHelper & UserService, (WorkspaceContext, UserContext, UserContext)] =
//    UserAspect ++ RoleChecker.roleAspect(roles*)
    ???
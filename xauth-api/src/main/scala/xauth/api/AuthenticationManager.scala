/*
 * Copyright (C) 2025-Present ZAuth.
 * This file is part of ZAuth, Multi-Tenant Authentication System.
 *
 * This software is released under the ZAuth License V1, which is based on the
 * GNU General Public License version 3 (GPLv3) as published by the Free Software
 * Foundation, with an additional "No SaaS" clause.
 *
 * You may redistribute and/or modify it under the terms of the GPLv3 as
 * published by the Free Software Foundation, with the added restriction that
 * this software may not be provided as a public network service (SaaS,
 * DBaaS, API, or similar) without prior written authorization from the author.
 *
 * THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT PERMITTED BY
 * APPLICABLE LAW. EXCEPT WHEN OTHERWISE STATED IN WRITING THE COPYRIGHT
 * HOLDERS AND/OR OTHER PARTIES PROVIDE THE PROGRAM "AS IS" WITHOUT WARRANTY
 * OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE. THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE PROGRAM
 * IS WITH YOU. SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF
 * ALL NECESSARY SERVICING, REPAIR OR CORRECTION.
 *
 * This software is released under ZAuth License V1.
 * See LICENSE for full terms.
 */
package xauth.api

import xauth.api.ClientResolver.CtxOut
import xauth.api.Main.validateEnv
import xauth.api.UserResolver.{CxtOut, Env}
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

/** Workspace information for recognized workspace requests. */
sealed class WorkspaceContext(val workspace: Workspace)

/** Client information for recognized client (http-basic) requests. */
sealed class ClientContext(override val workspace: Workspace, val client: Client) extends WorkspaceContext(workspace)

/** User information for recognized user requests. */
sealed class UserContext(override val workspace: Workspace, val user: User) extends WorkspaceContext(workspace)

/** Role information for recognized user roles requests. */
sealed class RoleContext(override val workspace: Workspace, override val user: User, val roles: Seq[AuthRole])
  extends UserContext(workspace, user)

object AuthenticationManager:

  type WorkspaceAspectEnv = WorkspaceResolver.Env
  type WorkspaceAspectCtxOut = WorkspaceResolver.CtxOut

  /**
   * Workspace Request Flow
   * Performs security checks for ingoing workspace requests.
   */
  val WorkspaceAspect: HandlerAspect[WorkspaceAspectEnv, WorkspaceAspectCtxOut] =
    WorkspaceResolver.aspect

  type ClientAspectEnv = WorkspaceAspectEnv & ClientResolver.Env
  type ClientAspectCtxOut = (WorkspaceAspectCtxOut, ClientResolver.CtxOut)

  /**
   * Client Request Flow
   * Performs security checks for ingoing client requests,
   * checks if http-basic authentication satisfies client requirements
   * like credentials and status.
   *
   * This aspect performs checks for:
   *   - Workspace 
   */
  val ClientAspect: HandlerAspect[ClientAspectEnv, ClientAspectCtxOut] =
    WorkspaceAspect ++ ClientResolver.aspect

  type UserAspectEnv = WorkspaceAspectEnv & UserResolver.Env
  type UserAspectCtxOut = (WorkspaceAspectCtxOut, UserResolver.CxtOut)

  /**
   * User Request Flow
   * Performs security checks for ingoing user requests,
   * checks if the request user is recognized, active
   * and if his credentials are valid.
   * 
   * This aspect performs checks for:
   *   - Workspace
   *   - User
   */
  val UserAspect: HandlerAspect[UserAspectEnv, UserAspectCtxOut] = 
    WorkspaceAspect ++ UserResolver.aspect

  type RoleAspectEnv = UserAspectEnv & RoleChecker.Env
  type RoleAspectCtxOut = (WorkspaceAspectCtxOut, UserResolver.CxtOut, RoleChecker.CtxOut)

  /**
   * Role Request Flow
   * Performs security checks for ingoing user roles requests,
   * checks if the recognized user matches at least one of the specified roles.
   *
   * This aspect performs checks for:
   *   - Workspace
   *   - User
   *   - Roles
   */
  def RoleAspect(roles: AuthRole*): HandlerAspect[RoleAspectEnv, RoleAspectCtxOut] =
    UserAspect ++ RoleChecker.roleAspect(roles*)
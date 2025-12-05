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
package xauth.api.auth

import xauth.api.auth.{RoleContext, UserContext}
import xauth.api.ext.error
import xauth.api.jwt.JwtHelper
import xauth.core.application.usecase.WorkspaceRegistry
import xauth.core.common.model.AuthRole
import xauth.core.domain.user.port.UserService
import zio.ZIO
import zio.http.Status.Forbidden
import zio.http.{Handler, Request, Response}

object RoleChecker:
  
  type Env = WorkspaceRegistry & JwtHelper & UserService & UserContext
  type CtxOut = RoleContext

  private type CxtIn = UserResolver.CtxOut

  private type RoleHandler = Handler[Env, Response, (CxtIn, Request), (CtxOut, Request)]

  def withRoles(rs: AuthRole*): RoleHandler =
    Handler.fromFunctionZIO[(CxtIn, Request)]:
      case (u, r) =>
        val roles = rs intersect u.user.roles
        if roles.isEmpty then
          ZIO fail Response.error(Forbidden, "role-checker:restricted", s"access restricted to: ${rs.mkString(", ")}")
        else
          ZIO succeed (RoleContext(u.workspace, u.user, roles), r)
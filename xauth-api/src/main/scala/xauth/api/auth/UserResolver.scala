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

import xauth.api.auth.{UserContext, WorkspaceContext}
import xauth.api.ext.error
import xauth.api.jwt.JwtHelper
import xauth.core.common.model.AuthStatus.Enabled
import xauth.core.domain.user.port.UserService
import zio.ZIO
import zio.http.Status.{BadRequest, Forbidden, InternalServerError, Unauthorized}
import zio.http.{Handler, Request, Response, Status}

object UserResolver:

  private val AuthHeader = "Authorization"
  private val AuthBearerRegex = "^Bearer\\s+(?<auth>.*)".r

  extension (r: Request)
    /**
     * Extracts bearer token from request [[AuthHeader]] header.
     *
     * @return Returns a [[Some[String]] object that contains
     *         the JWT bearer token if it is present into the
     *         request, returns [[None]] otherwise.
     */
    private def token: Option[String] =
      r.headers.get(AuthHeader) flatMap: s =>
        AuthBearerRegex.findFirstMatchIn(s) map:
          _.group("auth")

  type Env = JwtHelper & WorkspaceContext & UserService 
  type CtxOut = UserContext
  
  private type CxtIn = WorkspaceResolver.CtxOut
  
  private type UserHandler = Handler[Env, Response, (CxtIn, Request), (CtxOut, Request)]

  /** Decodes token from request header and creates a user context. */
  val handler: UserHandler = 
    Handler.fromFunctionZIO[(WorkspaceContext, Request)]:
      case (w, r) =>
        for
          jwtHelper <- ZIO.service[JwtHelper]
          userService <- ZIO.service[UserService]
  
          // retrieving token from http header
          token <- ZIO
            .fromOption(r.token)
            .orElseFail(Missing) // authentication token not found in request header
            .mapError(_ => Extraction) // unable to extract token from request header
  
          // decoding token
          (userId, workspaceId, _) <- jwtHelper
            .decodeToken(token)(using w.workspace)
            .mapError(_ => Parsing)
  
          // user retrieval
          context <-
            if w.workspace.id == workspaceId then
              userService
                .findById(userId)(using w.workspace)
                .mapError(t => Response.internalServerError(t.getMessage))
                .flatMap:
                  case Some(u) if u.status == Enabled =>
                    ZIO succeed new UserContext(w.workspace, u)
                  case Some(u) =>
                    ZIO.logWarning(s"invalid access token for user $userId from ${r.remoteAddress}") *>
                      ZIO.fail(Response.error(Forbidden, "user-resolver:not-enabled", s"account is currently '${u.status}'"))
                  case None =>
                    ZIO fail NotFound

            // token is not issued from the current workspace
            else ZIO fail Inconsistent
  
        yield (context, r)

  private lazy val Missing =
    Response.error(BadRequest, "user-resolver:missing", s"missing access token")

  private lazy val Extraction =
    Response.error(BadRequest, "user-resolver:extraction", s"unable to extract token from request header")

  private lazy val Parsing =
    Response.error(BadRequest, "user-resolver:parsing", s"unable to parse access token")

  private lazy val NotFound =
    Response.error(Unauthorized, "user-resolver:not-found", "user not found")

  private lazy val Inconsistent =
    Response.error(BadRequest, "user-resolver:inconsistent", "inconsistent workspace request")
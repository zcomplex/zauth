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

import xauth.api.auth.{ClientContext, ClientCredentials, WorkspaceContext}
import xauth.api.ext.error
import xauth.core.domain.client.port.ClientService
import xauth.util.ext.md5
import zio.ZIO
import zio.http.*
import zio.http.Status.{BadRequest, Forbidden, InternalServerError, Unauthorized}

import java.util.Base64

/**
 * Implements logic to perform a basic authentication.
 */
object ClientResolver:

  private val AuthHeader = "Authorization"
  private val AuthBasicRegex = "^Basic\\s+(?<auth>.*)".r

  extension (r: Request)
    /**
     * Extracts client credentials from request [[AuthHeader]] header.
     *
     * @return Returns a ClientCredentials object that contains the client credentials.
     */
    private def clientCredentials: Option[ClientCredentials] =
      r.headers.get(AuthHeader) flatMap: v =>
        AuthBasicRegex.findFirstMatchIn(v) map: m =>
          val auth = m.group("auth")
          val ss = new String(Base64.getDecoder.decode(auth)).split(":", 2)
          ClientCredentials(ss(0), ss(1))

  type Env = ClientService
  type CtxOut = ClientContext

  private type CxtIn = WorkspaceResolver.CtxOut

  private type ClientHandler = Handler[Env, Response, (CxtIn, Request), (CtxOut, Request)]

  val handler: ClientHandler =
    Handler.fromFunctionZIO[(WorkspaceContext, Request)]:
      case (w, r) =>
        for
          clientService <- ZIO.service[ClientService]

          // extracting client credentials from request header
          cc <- ZIO
            .fromOption(r.clientCredentials)
            .orElseFail(Missing)

          // checking credentials and creating the client context
          context <- clientService
            .find(cc.id)(using w.workspace)
            .mapError(t => Response.error(InternalServerError, "client-resolver:error", t.getMessage))
            .flatMap:
              case Some(c) if c.id == cc.id && c.secret == cc.secret.md5 =>
                ZIO succeed new ClientContext(w.workspace, c)
              case Some(_) =>
                ZIO.logWarning(s"invalid client credentials for ${cc.id}:${cc.secret}") *>
                  ZIO.fail(Wrong)
              case _ =>
                ZIO.logWarning(s"bad client credentials for ${cc.id}:${cc.secret}") *>
                  ZIO.fail(Invalid)
        yield (context, r)

  private lazy val Missing =
    Response.error(BadRequest, "client-resolver:missing", s"missing client credentials")

  private lazy val Wrong =
    Response.error(Forbidden, "client-resolver:wrong", "wrong client credentials")

  private lazy val Invalid =
    Response.error(Unauthorized, "client-resolver:invalid", "invalid client identifier")
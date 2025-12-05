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
package xauth.api.security

import xauth.api.security.WorkspaceContext
import xauth.api.ext.error
import xauth.core.application.usecase.WorkspaceRegistry
import xauth.core.domain.workspace.model.WorkspaceStatus
import xauth.util.Uuid
import zio.ZIO
import zio.http.Status.{BadRequest, Forbidden, Unauthorized}
import zio.http.{Handler, Request, Response}

/**
 * Implements logic to resolve workspace through the `X-Workspace-Id` http header in request.
 */
object WorkspaceResolver:
  private val WorkspaceHeader = "X-Workspace-Id"
  private val UuidRegex = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$".r

  extension (r: Request)
    /** Extracts workspace id from request X-Workspace-Id header. */
    private def workspaceId: Option[Uuid] =
      r.headers.get(WorkspaceHeader) flatMap: v =>
        UuidRegex.findFirstMatchIn(v) map (m => Uuid(m.group(0)))
        
  type Env = WorkspaceRegistry
  type CtxOut = WorkspaceContext

  private type WorkspaceHandler = Handler[Env, Response, Request, (CtxOut, Request)]

  /** Retrieves the workspace by its identifier in http header and creates the workspace context. */
  val handler: WorkspaceHandler =
    Handler.fromFunctionZIO[Request]: request =>
      for
        registry <- ZIO.service[WorkspaceRegistry]

        // extracting workspace identifier from request header
        wId <- ZIO
          .fromOption(request.workspaceId)
          .mapError(_ => Missing)

        // checking workspace and creating the context
        context <- registry
          .workspace(wId)
          .flatMap:
            case Some(w) if w.status == WorkspaceStatus.Enabled =>
              ZIO succeed new WorkspaceContext(w)
            case Some(w) =>
              ZIO fail Disabled
            case None =>
              ZIO fail Invalid
      yield (context, request)

  private lazy val Missing =
    Response.error(BadRequest, "workspace-resolver:missing", s"missing $WorkspaceHeader header")

  private lazy val Disabled =
    Response.error(Forbidden, "workspace-resolver:disabled", "workspace is disabled")

  private lazy val Invalid =
    Response.error(Unauthorized, "workspace-resolver:invalid", "invalid workspace identifier")
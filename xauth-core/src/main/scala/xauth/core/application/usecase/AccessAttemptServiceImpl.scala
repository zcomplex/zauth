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
package xauth.core.application.usecase

import xauth.core.common.model.AccessId
import xauth.core.domain.auth.model.AccessAttempt
import xauth.core.domain.auth.port.{AccessAttemptRepository, AccessAttemptService}
import xauth.core.domain.client.model.Client
import xauth.core.domain.user.model.User
import xauth.core.domain.workspace.model.Workspace
import xauth.util.Uuid
import zio.{Task, URLayer, ZIO, ZLayer}

import java.time.Instant

private class AccessAttemptServiceImpl(repository: AccessAttemptRepository) extends AccessAttemptService:

  /** Cleanups all user access attempts. */
  override infix def cleanup(user: User)(using w: Workspace): Task[Int] =
    repository cleanup user

  /** Retrieves the total user access attempts. */
  override infix def count(user: User)(using w: Workspace): Task[Int] =
    repository cleanup user

  /** Saves the access attempts for the given username and client. */
  override infix def save(user: User, client: Client, accessId: AccessId, remoteAddress: String)(using w: Workspace): Task[AccessAttempt] =
    val attempt = AccessAttempt(
      id = Uuid(),
      accessId = accessId.id,
      authType = accessId.authType,
      userId = user.id,
      clientId = client.id,
      remoteAddress = remoteAddress,
      createdAt = Instant.now
    )

    repository save attempt

object AccessAttemptServiceImpl:

  lazy val layer: URLayer[AccessAttemptRepository, AccessAttemptService] =
    ZLayer.fromZIO:
      ZIO.serviceWith[AccessAttemptRepository]: repository =>
        new AccessAttemptServiceImpl(repository)
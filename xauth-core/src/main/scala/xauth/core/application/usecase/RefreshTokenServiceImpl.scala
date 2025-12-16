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

import xauth.core.domain.auth.model.RefreshToken
import xauth.core.domain.auth.port.{RefreshTokenRepository, RefreshTokenService}
import xauth.core.domain.client.model.Client
import xauth.core.domain.user.model.User
import xauth.core.domain.workspace.model.Workspace
import zio.{Task, URLayer, ZIO, ZLayer}

import java.time.Instant

private class RefreshTokenServiceImpl(repository: RefreshTokenRepository) extends RefreshTokenService:

  /** Cleanups all user refresh tokens. */
  override infix def cleanup(u: User)(using w: Workspace): Task[Int] =
    repository cleanup u

  /** Retrieves the total user refresh tokens. */
  override infix def count(u: User)(using w: Workspace): Task[Int] =
    repository cleanup u

  /** Saves the refresh token for the given user and client. */
  override infix def save(t: String, u: User, c: Client)(using w: Workspace): Task[RefreshToken] =
    val now = Instant.now
    val expiresAt = now.plusSeconds(w.configuration.auth.jwt.expiration.refreshToken)
  
    val r = RefreshToken(
      token = t,
      clientId = c.id,
      userId = u.id,
      expiresAt = expiresAt,
      createdAt = now
    )

    repository save r

object RefreshTokenServiceImpl:

  lazy val layer: URLayer[RefreshTokenRepository, RefreshTokenService] =
    ZLayer.fromZIO:
      ZIO.serviceWith[RefreshTokenRepository]: repository =>
        new RefreshTokenServiceImpl(repository)
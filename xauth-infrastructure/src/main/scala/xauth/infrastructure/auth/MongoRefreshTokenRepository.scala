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
package xauth.infrastructure.auth

import reactivemongo.api.bson.BSONDocument
import xauth.core.domain.auth.model.RefreshToken
import xauth.core.domain.auth.port.RefreshTokenRepository
import xauth.core.domain.user.model.User
import xauth.core.domain.workspace.model.Workspace
import xauth.infrastructure.auth.RefreshTokenDo.*
import xauth.infrastructure.mongo.DefaultMongoClient
import xauth.infrastructure.mongo.WorkspaceCollection.RefreshToken as RefreshTokenC
import xauth.util.pagination.{PagedData, Pagination}
import zio.{Task, URLayer, ZIO, ZLayer}

private class MongoRefreshTokenRepository(mongo: DefaultMongoClient) extends RefreshTokenRepository:

  import bson.handler.given
  import xauth.infrastructure.mongo.bson.handler.uuidBsonHandler

  override infix def findAll(using p: Pagination): Task[PagedData[RefreshToken]] = ???

  override infix def delete(t: String)(using w: Workspace): Task[Boolean] = ???

  override infix def findAll(using w: Workspace): Task[Seq[RefreshToken]] = ???

  override infix def find(t: String)(using w: Workspace): Task[Option[RefreshToken]] = ???

  override infix def cleanup(u: User)(using w: Workspace): Task[Int] =
    mongo.collection(RefreshTokenC) flatMap:
      c => ZIO.fromFuture:
        implicit x => c
          .delete(ordered = false)
          .one(BSONDocument("userId" -> u.id))
          .map(_.n)

  override infix def create(r: RefreshToken)(using w: Workspace): Task[RefreshToken] =
    mongo.collection(RefreshTokenC) flatMap:
      c => ZIO.fromFuture:
        implicit x => c.insert.one(r.fromDomain) map { _ => r }

  override infix def update(r: RefreshToken)(using w: Workspace): Task[RefreshToken] =
    mongo.collection(RefreshTokenC) flatMap:
      c => ZIO.fromFuture:
        implicit x =>
          c.update
            .one(
              q = BSONDocument("_id" -> r.token),
              u = r.fromDomain,
              upsert = false
            )
            .map(_ => r)

  override infix def save(r: RefreshToken)(using w: Workspace): Task[RefreshToken] =
    mongo.collection(RefreshTokenC) flatMap:
      c => ZIO.fromFuture:
        implicit x =>
          c.update
            .one(
              q = BSONDocument("_id" -> r.token),
              u = r.fromDomain,
              upsert = true
            )
            .map(_ => r)

object MongoRefreshTokenRepository:

  lazy val layer: URLayer[DefaultMongoClient, RefreshTokenRepository] =
    ZLayer.fromZIO:
      ZIO.service[DefaultMongoClient] map:
        mongo => new MongoRefreshTokenRepository(mongo)
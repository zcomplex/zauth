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
import xauth.core.domain.auth.model.AccessAttempt
import xauth.core.domain.auth.port.AccessAttemptRepository
import xauth.core.domain.user.model.User
import xauth.core.domain.workspace.model.Workspace
import xauth.infrastructure.auth.AccessAttemptDo.*
import xauth.infrastructure.mongo.DefaultMongoClient
import xauth.infrastructure.mongo.WorkspaceCollection.AccessAttempt as AccessAttemptC
import xauth.util.Uuid
import xauth.util.pagination.{PagedData, Pagination}
import zio.{Task, URLayer, ZIO, ZLayer}

class MongoAccessAttemptRepository(mongo: DefaultMongoClient) extends AccessAttemptRepository:

  import bson.handler.given
  import xauth.infrastructure.mongo.bson.handler.uuidBsonHandler

  override infix def findAll(using p: Pagination): Task[PagedData[AccessAttempt]] = ???

  override infix def delete(id: Uuid)(using w: Workspace): Task[Boolean] = ???

  override infix def findAll(using w: Workspace): Task[Seq[AccessAttempt]] = ???

  override infix def find(id: Uuid)(using w: Workspace): Task[Option[AccessAttempt]] = ???

  override infix def cleanup(user: User)(using w: Workspace): Task[Int] =
    mongo.collection(AccessAttemptC) flatMap:
      c => ZIO.fromFuture:
        implicit x => c
          .delete(ordered = false)
          .one(BSONDocument("userId" -> user.id))
          .map(_.n)

  override infix def create(a: AccessAttempt)(using w: Workspace): Task[AccessAttempt] =
    mongo.collection(AccessAttemptC) flatMap:
      c => ZIO.fromFuture:
        implicit x => c.insert.one(a.fromDomain) map { _ => a }

  override infix def update(a: AccessAttempt)(using w: Workspace): Task[AccessAttempt] =
    mongo.collection(AccessAttemptC) flatMap:
      c => ZIO.fromFuture:
        implicit x =>
          c.update
            .one(
              q = BSONDocument("_id" -> a.id),
              u = a.fromDomain,
              upsert = false
            )
            .map(_ => a)

  override infix def save(a: AccessAttempt)(using w: Workspace): Task[AccessAttempt] =
    mongo.collection(AccessAttemptC) flatMap:
      c => ZIO.fromFuture:
        implicit x =>
          c.update
            .one(
              q = BSONDocument("_id" -> a.id),
              u = a.fromDomain,
              upsert = true
            )
            .map(_ => a)

object MongoAccessAttemptRepository:

  lazy val layer: URLayer[DefaultMongoClient, AccessAttemptRepository] =
    ZLayer.fromZIO:
      ZIO.service[DefaultMongoClient] map:
        mongo => new MongoAccessAttemptRepository(mongo)
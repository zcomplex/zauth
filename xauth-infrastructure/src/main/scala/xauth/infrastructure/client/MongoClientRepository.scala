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
package xauth.infrastructure.client

import reactivemongo.api.bson.BSONDocument
import xauth.core.domain.client.model.Client
import xauth.core.domain.client.port.ClientRepository
import xauth.core.domain.workspace.model.Workspace
import xauth.infrastructure.client.ClientDo.*
import xauth.infrastructure.mongo.DefaultMongoClient
import xauth.infrastructure.mongo.WorkspaceCollection.Client as ClientC
import zio.{Task, URLayer, ZIO, ZLayer}

class MongoClientRepository(mongo: DefaultMongoClient) extends ClientRepository:

  import bson.handler.given

  /** Deletes entity by its identifier. */
  override infix def delete(id: String)(using w: Workspace): Task[Boolean] = ???

  /** Finds all entities. */
  override infix def findAll(using w: Workspace): Task[Seq[Client]] = ???

  /** Finds entity by its identifier. */
  override infix def find(id: String)(using w: Workspace): Task[Option[Client]] =
    mongo.collection(ClientC) flatMap:
      c =>
        val s = BSONDocument("_id" -> id)
        ZIO.fromFuture(implicit _ => c.find(s).one[ClientDo].map(_.map(_.toDomain)))

  override infix def create(c: Client)(using w: Workspace): Task[Client] = ???

  override infix def update(c: Client)(using w: Workspace): Task[Client] = ???

  override infix def save(c: Client)(using w: Workspace): Task[Client] =
    mongo.collection(ClientC).flatMap:
      collection => ZIO.fromFuture:
        implicit _ => collection.insert.one(c.fromDomain) map { _ => c }

object MongoClientRepository:

  lazy val layer: URLayer[DefaultMongoClient, MongoClientRepository] =
    ZLayer.fromZIO:
      ZIO.service[DefaultMongoClient] map:
        new MongoClientRepository(_)
  
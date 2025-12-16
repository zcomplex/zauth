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
package xauth.infrastructure.code

import xauth.core.domain.code.model.AccountCode
import xauth.core.domain.code.port.AccountCodeRepository
import xauth.core.domain.workspace.model.Workspace
import xauth.infrastructure.code.AccountCodeDo.*
import xauth.infrastructure.mongo.DefaultMongoClient
import xauth.infrastructure.mongo.WorkspaceCollection.Code as CodeC
import zio.{Task, URLayer, ZIO, ZLayer}

private final class MongoAccountCodeRepository(mongo: DefaultMongoClient) extends AccountCodeRepository:

  import bson.given

  /** Deletes account code by code. */
  override infix def delete(code: String)(using w: Workspace): Task[Boolean] = ???

  /** Finds all account codes. */
  override infix def findAll(using w: Workspace): Task[Seq[AccountCode]] = ???

  /** Finds account code by code. */
  override infix def find(code: String)(using w: Workspace): Task[Option[AccountCode]] = ???

  override infix def create(a: AccountCode)(using w: Workspace): Task[AccountCode] = ???

  override infix def update(a: AccountCode)(using w: Workspace): Task[AccountCode] = ???

  /** Saves account code on persistence system. */
  override infix def save(a: AccountCode)(using w: Workspace): Task[AccountCode] =
    mongo.collection(CodeC) flatMap:
      c => ZIO.fromFuture:
        implicit _ => c.insert.one(a.fromDomain) map { _ => a }

object MongoAccountCodeRepository:
  
  lazy val layer: URLayer[DefaultMongoClient, AccountCodeRepository] =
    ZLayer.fromZIO:
      ZIO.service[DefaultMongoClient] map:
        mongo => new MongoAccountCodeRepository(mongo)
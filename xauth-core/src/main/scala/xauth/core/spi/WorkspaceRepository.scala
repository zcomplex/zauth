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
package xauth.core.spi

import xauth.core.domain.workspace.model.Workspace
import zio.Task

trait WorkspaceRepository[T, Id]:

  /** Deletes entity by its identifier. */
  infix def delete(id: Id)(using w: Workspace): Task[Boolean]

  /** Finds all entities. */
  infix def findAll(using w: Workspace): Task[Seq[T]]

  /** Finds entity by its identifier. */
  infix def find(id: Id)(using w: Workspace): Task[Option[T]]

  /** Creates entity on the persistence system. */
  infix def create(e: T)(using w: Workspace): Task[T]

  /** Updates entity on the persistence system. */
  infix def update(e: T)(using w: Workspace): Task[T]

  /** Saves the entity on the persistence system. */
  infix def save(e: T)(using w: Workspace): Task[T]
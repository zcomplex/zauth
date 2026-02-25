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
package xauth.core.domain.event.port

import xauth.core.domain.event.model.Event.WorkspaceEvent
import xauth.core.domain.workspace.model.Workspace
import xauth.util.Uuid
import zio.Task

trait WorkspaceEventService:

  /** Performs the cleanup of all events from the persistence system. */
  infix def cleanup(using w: Workspace): Task[Int]

  /** Finds the event by its identifier. */
  infix def find(id: Uuid)(using w: Workspace): Task[Option[WorkspaceEvent]]

  /** Finds all events related to the same aggregate identifier. */
  infix def findByAggregateId(id: Uuid)(using w: Workspace): Task[Seq[WorkspaceEvent]]
  
  /** Publishes the event on the system bus, without persist it. */
  infix def publish(e: WorkspaceEvent)(using w: Workspace): Task[Unit]

  /** Publishes all events on the system bus, without persist them. */
  infix def publish(e: Seq[WorkspaceEvent])(using w: Workspace): Task[Unit]

  /** Publishes and saves the given event. */
  infix def publishAndSave(e: WorkspaceEvent)(using w: Workspace): Task[Unit]

  /** Publishes and saves all events into the sequence. */
  infix def publishAndSave(s: Seq[WorkspaceEvent])(using w: Workspace): Task[Unit]

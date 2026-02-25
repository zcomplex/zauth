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
package xauth.core.domain.event.model

import xauth.core.common.model.AccessId.Id as AccessId
import xauth.core.common.model.AuthType
import xauth.core.domain.event.model.Event.*
import xauth.core.domain.event.model.EventType.{SystemEventType, WorkspaceEventType}
import xauth.util.Uuid

import java.time.Instant

/**
 * Represents the event, captures a significant occurrence in the system, recording its context,
 * details and relationships for auditing, processing, and reliable replay.
 * 
 * @param id Event identifier.
 * @param eType Event type.
 * @param ref Reference, If this field is set, it tells us which object the event refers to.
 *            For example, for an authentication event, the ref field would be useful to refer
 *            to the user who logged in.
 * @param aggregate The aggregate field represents the logical stream or entity group to which this event belongs,
 *                  it allows systems to correlate multiple events that are part of the same workflow, transaction,
 *                  or domain object.
 * @param payload Holds domain information that describe the specific event, it tell us what happened.
 *                Used by domain consumers, never contains environmental information.
 * @param context Describes external and transversal conditions in which the event happened,
 *                the context holds environmental information.
 *                Cross-domain, not versioned and doesn't change the meaning of the event.
 * @param metadata Technical and infrastructure information, it doesn't hold domain information.
 *                 Useful to know how the event has been transported.
 * @param occurredAt   When the event happened.
 * @param registeredAt When the event has been persisted.
 */
trait Event[A <: EventType]:
  val id: Uuid
  val eType: A
  val ref: Ref
  val aggregate: Option[Aggregate]
  val payload: Payload
  val context: Option[Context]
  val metadata: Option[Metadata]
  val occurredAt: Instant
  val registeredAt: Instant

object Event:

  final case class SystemEvent
  (
    override val id: Uuid,
    override val eType: SystemEventType,
    override val ref: Ref,
    override val aggregate: Option[Aggregate],
    override val payload: Payload,
    override val context: Option[Context],
    override val metadata: Option[Metadata],
    override val occurredAt: Instant,
    override val registeredAt: Instant
  ) extends Event[SystemEventType]

  // todo: evaluate to store temporary information about workspace but not for persistence
  final case class WorkspaceEvent
  (
    override val id: Uuid,
    override val eType: WorkspaceEventType,
    override val ref: Ref,
    override val aggregate: Option[Aggregate],
    override val payload: Payload,
    override val context: Option[Context],
    override val metadata: Option[Metadata],
    override val occurredAt: Instant,
    override val registeredAt: Instant
  ) extends Event[WorkspaceEventType]

  object WorkspaceEvent:

    def authenticationSucceeded
    (
      userId: Uuid,
      accessId: AccessId,
      authType: AuthType,
      aggregate: Option[Aggregate] = None,
      context: Option[Context] = None,
      metadata: Option[Metadata] = None,
      occurredAt: Instant = Instant.now
    ): WorkspaceEvent = Event.WorkspaceEvent(
      id = Uuid(),
      eType = WorkspaceEventType.AuthenticationSucceeded,
      ref = Event.Ref(userId, RefType.User),
      aggregate = aggregate,
      payload = Event.Payload.WorkspaceEventContent.AuthenticationSucceeded(
        version = 1,
        content = Event.Payload.WorkspaceEventContent.AuthenticationSucceeded.Content(
          accessId = accessId,
          authType = authType
        )
      ),
      context = context,
      metadata = metadata,
      occurredAt = occurredAt,
      registeredAt = Instant.now
    )

  enum AggregateType:
    case MyFlow

  case class Aggregate(id: Uuid, aType: AggregateType)

  sealed trait Payload:
    type Content
    def version: Int
    def content: Content

  object Payload:

    object WorkspaceEventContent:

      final case class AuthenticationSucceeded
      (
        version: Int,
        content: AuthenticationSucceeded.Content
      ) extends Payload:
        type Content = AuthenticationSucceeded.Content
  
      object AuthenticationSucceeded:
        final case class Content(accessId: AccessId, authType: AuthType)

  enum RefType:
    case User

  case class Ref(id: Uuid, rType: RefType) // todo: id: String

  trait Context
  trait Metadata
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

import xauth.core.application.usecase.WorkspaceRegistry.WContext
import xauth.core.domain.workspace.model.Workspace
import xauth.core.domain.workspace.port.WorkspaceRepository
import xauth.core.spi.MessagingProvider
import xauth.core.spi.MessagingProvider.ProviderRegistry
import xauth.util.{Registry, Uuid}
import zio.stm.TMap
import zio.{RLayer, UIO, ZIO, ZLayer}

import scala.reflect.ClassTag

// todo: evaluate to remove registry trait
// todo: evaluate to make trait under the domain
class WorkspaceRegistry(registry: ProviderRegistry, data: TMap[Uuid, WContext]) extends Registry[Uuid, WContext](data):

  infix def workspace(id: Uuid): UIO[Option[Workspace]] =
    get(id).map(_.map(_.workspace))

  infix def context(id: Uuid): UIO[Option[WContext]] =
    get(id)

  infix def register(w: Workspace): UIO[Unit] =
    for
      ps <- registry.providers(using w)
      _  <- put(w.id, (w, ps))
      _  <- ZIO logInfo s"registered workspace '${w.slug}' with ${ps.size} providers"
    yield ()

  infix def remove(w: Workspace): UIO[Unit] = delete(w.id)

  def entries: UIO[Int] = size

  def messaging[A <: MessagingProvider](using w: Workspace, ct: ClassTag[A]): UIO[Option[A]] =
    get(w.id) map:
      _.flatMap:
        _.providers.collectFirst:
          case p if ct.runtimeClass.isAssignableFrom(p.getClass) => p.asInstanceOf[A]

object WorkspaceRegistry:

  type WContext = (workspace: Workspace, providers: Set[MessagingProvider])

  private type WEntry = (id: Uuid, context: WContext)

  val layer: RLayer[ProviderRegistry & WorkspaceRepository, WorkspaceRegistry] =
    ZLayer.fromZIO:
      for
        registry   <- ZIO.service[ProviderRegistry]
        repository <- ZIO.service[WorkspaceRepository]

        workspaces <- repository.findAll

        entries    <- ZIO.foreach(workspaces): w =>
          for
            providers <- registry.providers(using w)
            entry <- ZIO.succeed[WEntry]:
              w.id -> (w, providers)

            _ <- ZIO logInfo s"loading workspace '${w.slug}' with ${entry.context.providers.size} providers"
          yield entry

        // todo: use zio cache and refresh workspaces (or use a queue for workspaces that need to be refreshed)
        data <- TMap
          .fromIterable[Uuid, WContext](entries.map(e => (e.id, e.context)))
          .commit

        n <- data.size.commit
        _ <- ZIO logInfo s"loaded $n workspaces into the registry"

      yield new WorkspaceRegistry(registry, data)
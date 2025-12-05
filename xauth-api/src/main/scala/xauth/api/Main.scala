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
package xauth.api

import xauth.api.Layer as layer
import xauth.api.controller.auth.AuthController
import xauth.api.controller.info.InfoController
import xauth.api.controller.tenant.TenantController
import xauth.core.application.usecase.*
import xauth.core.domain.client.port.ClientService
import xauth.core.domain.code.port.{AccountCodeRepository, AccountCodeService}
import xauth.core.domain.system.port.{SystemService, SystemSettingRepository}
import xauth.core.domain.tenant.port.TenantService
import xauth.core.domain.workspace.port.{WorkspaceRepository, WorkspaceService}
import xauth.core.spi.MessagingProvider.ProviderRegistry
import xauth.core.spi.{AccountEventDispatcher, MessagingService, TemplateService}
import xauth.generated.BuildInfo
import xauth.infrastructure.mongo.*
import zio.*
import zio.http.*
import zio.http.Method.GET

object Main extends ZIOAppDefault:

  //  import scala.math.Fractional.Implicits.infixFractionalOps
  private val Home = GET / Root -> handler(Response.text(s"X-Auth ${BuildInfo.Version}"))


  private def routes(authC: AuthController) =
    (Routes.empty :+ Home)
      ++ InfoController.routes   // /info
      ++ authC.routes            // /auth    -> Authentication routes
      ++ TenantController.routes
  // /tenants
      // todo: get /health
      // todo: get /init/configure or /system/configure
      // todo: /system/tenants
  
//  val serverConfig = Server.Config.default
//    .port(1234)
//    .keepAlive(true)
//    .idleTimeout(30.seconds)
//    .requestDecompression(true)
//    .maxHeaderSize(8 * 1024)

  def run: ZIO[Any, Throwable, Nothing] = {
    val effect = for
      // First initialization
      sys <- ZIO.service[SystemService]
      _   <- sys.init
      // Controller layers
      authC <- ZIO.service[AuthController]
      // Routes to serve
      allRoutes = routes(authC)
      // Starting service
      _  <- Server.serve(allRoutes)
    yield ()

    // todo: handle graceful shutdown
    // todo: handle idle timeout

    effect
      .as(ZIO.never)
      .flatten
      .provide(
        Server.default,

        layer.accountEventDispatcher,
        layer.config,
        layer.environment,
        layer.providerRegistry,
        layer.workspaceRegistry,

        layer.persistence.mongodb.client,
        layer.persistence.mongodb.repository.accountCode,
        layer.persistence.mongodb.repository.client,
        layer.persistence.mongodb.repository.systemSetting,
        layer.persistence.mongodb.repository.tenant,
        layer.persistence.mongodb.repository.user,
        layer.persistence.mongodb.repository.workspace,

        layer.service.accountCode,
        layer.service.client,
        layer.service.messaging,
        layer.service.system,
        layer.service.template,
        layer.service.tenant,
        layer.service.user,
        layer.service.workspace,

        // controllers
        layer.controller.auth
      )
  }
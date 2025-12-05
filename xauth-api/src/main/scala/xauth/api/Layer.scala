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

import io.circe.*
import io.circe.config.parser.*
import io.circe.generic.auto.*
import xauth.api.controller.auth.AuthController
import xauth.core.application.usecase.*
import xauth.core.common.model.ContactType
import xauth.core.domain.client.port.{ClientRepository, ClientService}
import xauth.core.domain.code.port.{AccountCodeRepository, AccountCodeService}
import xauth.core.domain.configuration.model.{Configuration as AppConf, *}
import xauth.core.domain.system.port.{SystemService, SystemSettingRepository}
import xauth.core.domain.tenant.port.{TenantRepository, TenantService}
import xauth.core.domain.user.model.{UserContact, UserInfo}
import xauth.core.domain.user.port.{UserRepository, UserService}
import xauth.core.domain.workspace.model.*
import xauth.core.domain.workspace.port.{WorkspaceRepository, WorkspaceService}
import xauth.core.spi.MessagingProvider.ProviderRegistry
import xauth.core.spi.env.{TimeService, UuidService}
import xauth.core.spi.{AccountEventDispatcher, MessagingService, TemplateService, env}
import xauth.infrastructure.client.MongoClientRepository
import xauth.infrastructure.code.MongoAccountCodeRepository
import xauth.infrastructure.messaging.provider.ProviderRegistryImpl
import xauth.infrastructure.mongo.*
import xauth.infrastructure.setting.MongoSystemSettingRepository
import xauth.infrastructure.tenant.MongoTenantRepository
import xauth.infrastructure.user.MongoUserRepository
import xauth.infrastructure.workspace.MongoWorkspaceRepository
import zio.{RLayer, TaskLayer, ULayer, URLayer, ZIO, ZLayer}

import java.util.Locale

object Layer:
  private given Decoder[Locale] = Decoder.decodeString map Locale.forLanguageTag
  private given Decoder[ContactType] = Decoder.decodeString map ContactType.fromValue
  
  val config: TaskLayer[AppConf] =
    ZLayer.fromZIO:
      for
        json <- ZIO.succeed(os.read(os.pwd / "conf" / "application.conf"))
        config <- ZIO.fromEither(decode[AppConf](json))
      yield config

  val environment: ULayer[env.Environment] = TimeService.layer ++ UuidService.layer
    >>> DefaultEnvironment.layer

  object service:
    val accountCode: URLayer[env.Environment & AccountCodeRepository, AccountCodeService] =
      AccountCodeServiceImpl.layer

    val client: URLayer[ClientRepository, ClientService] =
      ClientServiceImpl.layer

    val template: TaskLayer[TemplateService] =
      TemplateServiceImpl.layer(os.pwd / "conf" / "template")

    val messaging: RLayer[TemplateService & WorkspaceRegistry, MessagingService] = template
      >>> MessagingServiceImpl.layer

    val tenant: URLayer[TenantRepository, TenantService] =
      TenantServiceImpl.layer

    val user: URLayer[UserRepository & AccountEventDispatcher, UserServiceImpl] =
      UserServiceImpl.layer

    val workspace: URLayer[WorkspaceRegistry & WorkspaceRepository & AppConf, WorkspaceService] =
      WorkspaceServiceImpl.layer

    val system: URLayer[SystemSettingRepository & TenantService & WorkspaceService & ClientService & UserService & AppConf, SystemService] =
      SystemServiceImpl.layer

  object persistence:
    object mongodb:
      val client: RLayer[AppConf, DefaultMongoClient] = config
        >>> DefaultDriver.layer
        >>> DefaultMongoClient.layer

      object repository:
        val accountCode: URLayer[DefaultMongoClient, AccountCodeRepository] =
          MongoAccountCodeRepository.layer

        val tenant: URLayer[DefaultMongoClient, TenantRepository] =
          MongoTenantRepository.layer

        val workspace: URLayer[DefaultMongoClient, WorkspaceRepository] =
          MongoWorkspaceRepository.layer

        val client: URLayer[DefaultMongoClient, ClientRepository] =
          MongoClientRepository.layer

        val systemSetting: URLayer[DefaultMongoClient, SystemSettingRepository] =
          MongoSystemSettingRepository.layer

        val user: URLayer[DefaultMongoClient, UserRepository] =
          MongoUserRepository.layer

  val workspaceRegistry: RLayer[ProviderRegistry & WorkspaceRepository, WorkspaceRegistry] =
    WorkspaceRegistry.layer

  val accountEventDispatcher: URLayer[AccountCodeService & MessagingService, AccountEventDispatcher] =
    AccountEventDispatcherImpl.layer

  val providerRegistry: ULayer[ProviderRegistry] =
    ProviderRegistryImpl.layer

  object controller:
    val auth: ULayer[AuthController] = AuthController.layer
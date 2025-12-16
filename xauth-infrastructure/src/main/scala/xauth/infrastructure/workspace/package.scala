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
package xauth.infrastructure

import io.circe.Json
import reactivemongo.api.bson.{BSONDocument, BSONDocumentHandler, BSONHandler, BSONString, BSONValue, Macros}
import xauth.core.domain.workspace.model.*
import xauth.core.domain.workspace.model.ProviderConf.PConf

import scala.util.{Success, Try}

package object workspace:

  object bson:
    object handler:

      import xauth.infrastructure.mongo.bson.ext.{toBson, toJson}
      import xauth.infrastructure.mongo.bson.handler.given

      given workspaceStatusBsonHandler: BSONHandler[WorkspaceStatus] = new BSONHandler[WorkspaceStatus]:
        override def readTry(b: BSONValue): Try[WorkspaceStatus] = b.asTry[BSONString] map { s => WorkspaceStatus.fromValue(s.value) }
        override def writeTry(s: WorkspaceStatus): Try[BSONValue] = Success(BSONString(s.value))

      given routesConfigurationBsonHandler: BSONDocumentHandler[RoutesConf] = Macros.handler[RoutesConf]

      given smtpConfigurationBsonHandler: BSONDocumentHandler[SmtpConf] = Macros.handler[SmtpConf]

      given expirationBsonHandler: BSONDocumentHandler[Expiration] = Macros.handler[Expiration]

      given encryptionBsonHandler: BSONDocumentHandler[Encryption] = Macros.handler[Encryption]

      given databaseConfBsonHandler: BSONDocumentHandler[DatabaseConf] = Macros.handler[DatabaseConf]

      given authConfBsonHandler: BSONDocumentHandler[AuthConf] = Macros.handler[AuthConf]

      given frontEndConfigurationBsonHandler: BSONDocumentHandler[FrontEndConf] = Macros.handler[FrontEndConf]

      given pConfBsonHandler: BSONDocumentHandler[PConf] = new BSONDocumentHandler[PConf]:
        override def readDocument(b: BSONDocument): Try[PConf] =
          b.asTry[BSONDocument] map:
            _.elements
              .collect:
                e => e.name -> e.value.toJson
              .toMap

        override def writeTry(c: PConf): Try[BSONDocument] =
          Success:
            BSONDocument:
              c.collect:
                case (k, v: Json) => k -> v.toBson

      given providerConfigurationBsonHandler: BSONDocumentHandler[ProviderConf] = Macros.handler[ProviderConf]

      given messagingConfigurationBsonHandler: BSONDocumentHandler[MessagingConf] = Macros.handler[MessagingConf]

      given mailConfigurationBsonHandler: BSONDocumentHandler[MailConf] = Macros.handler[MailConf]

      given jwtBsonHandler: BSONDocumentHandler[Jwt] = Macros.handler[Jwt]

      given workspaceConfigurationBsonHandler: BSONDocumentHandler[WorkspaceConf] = Macros.handler[WorkspaceConf]

      given companyConfBsonHandler: BSONDocumentHandler[Company] = Macros.handler[Company]

      given workspaceBsonHandler: BSONDocumentHandler[WorkspaceDo] = Macros.handler[WorkspaceDo]
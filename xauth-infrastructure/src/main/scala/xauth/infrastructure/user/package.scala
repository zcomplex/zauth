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

import io.circe.Encoder
import io.circe.generic.semiauto.*
import reactivemongo.api.bson.{BSONDocumentHandler, BSONValue, Macros}
import xauth.core.common.model.Permission
import xauth.core.domain.user.model.{AppInfo, UserContact, UserInfo}

import scala.util.{Success, Try}

package object user:

  object bson:
    object handler:
      
      import xauth.infrastructure.mongo.bson.handler.given
  
      given appInfoBsonHandler: BSONDocumentHandler[AppInfo] = Macros.handler[AppInfo]
      
      given userContactBsonHandler: BSONDocumentHandler[UserContact] = Macros.handler[UserContact]
      given userInfoBsonHandler: BSONDocumentHandler[UserInfo] = Macros.handler[UserInfo]
      
      given userBsonHandler: BSONDocumentHandler[UserDo] = Macros.handler[UserDo]
  
  object json:
    
    given permission: Encoder[Permission] = Encoder.encodeString.contramap(_.value)
    given appInfo: Encoder[AppInfo] = deriveEncoder
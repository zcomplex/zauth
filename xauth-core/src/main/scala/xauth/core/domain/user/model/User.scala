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
package xauth.core.domain.user.model

import xauth.core.common.model.ContactType.*
import xauth.core.common.model.{AuthRole, AuthStatus, ContactType, Permission}
import xauth.util.Uuid

import java.time.Instant

/** Defines user information. */
case class UserInfo
(
  firstName: String,
  lastName: String,
  company: String,
  contacts: Seq[UserContact]
)

/** Defines minimum information of a contact */
case class UserContact
(
  kind: ContactType,
  value: String,
  description: Option[String],
  trusted: Boolean
)

/** Defines application information */
case class AppInfo
(
  name: String,
  permissions: Set[Permission]
)

case class User
(
  id: Uuid,
  username: String,
  password: String,
  salt: String,
  parentId: Option[Uuid],
  roles: Seq[AuthRole],
  applications: Seq[AppInfo] = Nil,
  status: AuthStatus,
  description: Option[String],
  info: UserInfo,
  createdBy: Uuid,
  createdAt: Instant,
  updatedBy: Uuid,
  updatedAt: Instant
):

  def contact(t: ContactType, trusted: Boolean = true): Option[String] =
    info.contacts
      .find(c => c.kind == t && c.trusted == trusted)
      .map(_.value)

  def email(trusted: Boolean = true): Option[String] = 
    contact(Email, trusted)

  def mobileNumber(trusted: Boolean = true): Option[String] =
    contact(MobileNumber, trusted)
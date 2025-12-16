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

import com.lambdaworks.crypto.SCryptUtil.*
import xauth.core.common.model.AuthStatus.New
import xauth.core.common.model.{AuthRole, AuthStatus}
import xauth.core.domain.user.model.{AppInfo, User, UserInfo}
import xauth.core.domain.user.port.{UserRepository, UserService}
import xauth.core.domain.workspace.model.Workspace
import xauth.core.spi.{AccountEvent, AccountEventDispatcher}
import xauth.util.Uuid
import xauth.util.ext.random
import xauth.util.pagination.{PagedData, Pagination}
import zio.{Task, URLayer, ZIO, ZLayer}

import java.time.Instant

class UserServiceImpl(repository: UserRepository, dispatcher: AccountEventDispatcher) extends UserService:

  override def activate(code: String)(using w: Workspace): Task[Boolean] = ???

  override def childrenOf(id: Uuid)(using w: Workspace, p: Pagination): Task[PagedData[User]] = ???

  override def create(u: User)(using w: Workspace): Task[User] = ???

  override def create(username: String, password: String, description: Option[String], parentId: Option[Uuid], userInfo: UserInfo, status: AuthStatus, applications: List[AppInfo], roles: AuthRole*)(using w: Workspace): Task[User] =

    val now = Instant.now

    val encryption = cryptWithSalt(password)

    val user = User(
      id = Uuid(),
      username = username,
      password = encryption.hash,
      salt = encryption.salt,
      parentId = parentId,
      roles = roles.toList,
      status = status,
      description = description,
      applications = applications,
      info = userInfo,
      createdBy = Uuid.Zero, // todo: read it by context
      createdAt = now,
      updatedBy = Uuid.Zero, // todo: read it by context
      updatedAt = now
    )

    for
      u <- repository save user
      _ <- ZIO
        .when(user.status == New):
          dispatcher.dispatch(AccountEvent.UserRegistered(u, w)).forkDaemon
    yield u

  override def create(username: String, password: String, description: Option[String], parentId: Option[Uuid], userInfo: UserInfo)(using w: Workspace): Task[User] = ???

  /**
   * Checks if hashed string has been encrypted for given salt and string.
   *
   * @param s  The salt.
   * @param ss The string to check with salt `s`.
   * @param hs The hashed string.
   * @return Returns `true` if `hs` has been encrypted by the given `s` salt and
   *         the string `s`, returns false otherwise.
   */
  override def checkWithSalt(s: String, ss: String, hs: String): Boolean = check(s + ss, hs)

  /**
   * Cyphers the string with a generated salt.
   *
   * @return Returns a [[Tuple2]] that contains generated salt and hashed string
   *         using `scrypt` encryption algorithm.
   */
  override def cryptWithSalt(s: String): (salt: String, hash: String) =
    val salt = (('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9')).random(79)
    val pass = scrypt(salt + s, 16, 16, 16) // 79 output bytes
    (salt, pass)

  override def findAll(implicit w: Workspace, p: Pagination): Task[PagedData[User]] = ???

  override def findById(id: Uuid)(using w: Workspace): Task[Option[User]] = ???

  override def findByUsername(username: String)(using w: Workspace): Task[Option[User]] =
    repository findByUsername username

  override def delete(id: Uuid)(using w: Workspace): Task[Boolean] = ???

  override def resetPassword(id: Uuid, password: String)(using w: Workspace): Task[Boolean] = ???

  override def trustAccount(u: User)(using w: Workspace): Task[Unit] = ???

  override def trustContact(c: String)(using w: Workspace): Task[Boolean] = ???

  override def updateApplications(id: Uuid, applications: AppInfo*)(using w: Workspace): Task[Option[User]] = ???

  override def updateRoles(id: Uuid, roles: AuthRole*)(using w: Workspace): Task[Option[User]] = ???

  override def updateStatusById(id: Uuid, status: AuthStatus)(using w: Workspace): Task[Option[User]] =
    repository.find(id) flatMap:
      case Some(u) => repository
        .save(u.copy(status = status))
        .option
      case None => ZIO.none

  override def updateStatusByUsername(u: String, status: AuthStatus)(using w: Workspace): Task[Option[User]] = ???

object UserServiceImpl:

  val layer: URLayer[UserRepository & AccountEventDispatcher, UserServiceImpl] =
    ZLayer.fromZIO:
      for
        repository <- ZIO.service[UserRepository]
        dispatcher <- ZIO.service[AccountEventDispatcher]
      yield new UserServiceImpl(repository, dispatcher)
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
package xauth.api.controller.auth

import xauth.api.*
import xauth.api.controller.AbstractController
import xauth.api.controller.auth.AuthController.{InvalidWorkspace, OutOfService, WorkspaceError, WorkspaceNotEnabled}
import xauth.api.ext.error
import xauth.api.jwt.JwtHelper
import xauth.api.model.auth.*
import xauth.api.model.ziojson.auth.given
import xauth.api.security.{ClientContext, ClientCredentials}
import xauth.core.common.model.AccessId
import xauth.core.common.model.AuthStatus.Enabled
import xauth.core.domain.auth.port.{AccessAttemptService, RefreshTokenService}
import xauth.core.domain.user.port.UserService
import xauth.core.domain.workspace.model.Workspace
import zio.*
import zio.ZIO.*
import zio.http.*
import zio.http.Method.{GET, POST}
import zio.http.Status.*
import zio.json.*
import zio.json.ast.Json
import zio.schema.{Schema, derived}

//import io.circe.schema.Schema
//import io.circe.parser._
//import io.circe.generic.auto._
//import io.circe._
//import io.circe.generic.semiauto._

sealed class AuthController extends AbstractController:

  trait TokenError:
    val message: String

  private final case class InvalidPassword(override val message: String) extends TokenError
  private final case class NotEnabledUser(override val message: String) extends TokenError
  private final case class UserNotFound(override val message: String) extends TokenError
  
//  type JsError = Json
//
//  private final def err(m: String) = Json.obj("message" -> Json.fromString(m))


  val res = TokenRs(
    "tokenType",
    "accessToken",
    0,
    "refreshToken"
  )

//  val intStringRequestHandler: Handler[(Int, String), Nothing, TokenRq, Response] =
//    Handler.fromFunctionZIO[TokenRq] { (req: TokenRq) =>
//      ZIO.serviceWith[(Int, String)] { case (n, s) =>
////        Response.text(s"Received the $n and $s values from the output context!")
////          ZIO.succeed(res)
//        res
//      }
//    }
//
//  intStringRequestHandler.@@(auth.ClientAspect)
//  import zio.json._

  final case class ErrorResponse(message: String) derives JsonCodec, zio.schema.Schema

  val authEnd = GET / "test" / "client" -> (auth.ClientHandler >>> Handler.fromFunctionZIO[(ClientContext, Request)] {
    case (c, r) =>

      val json = Json.Obj(
        "workspaceId" -> Json.Str(c.workspace.id.stringValue),
        "clientId" -> Json.Str(c.client.id)
      )

      ZIO succeed Response.json(json.toJson)
  })

  private lazy val NotAvailable =
    Response.error(ServiceUnavailable, "auth.token:unavailable", "unavailable service, please try later")

  private lazy val InvalidCredentials =
    Response.error(Unauthorized, "auth.token:invalid", "invalid user credentials")

  private val Token = POST / "auth" / "token" -> auth
    .ClientHandler
    .andThen:
      Handler.fromFunctionZIO[(ClientContext, Request)]:
        case (c, r) =>

          given w: Workspace = c.workspace

          // todo: validation/json-schema validation
          val effect = for
            service <- ZIO.service[UserService]
            attempts <- ZIO.service[AccessAttemptService]
            refresh <- ZIO.service[RefreshTokenService]
            jwtHelper <- ZIO.service[JwtHelper]

            json <- r
              .body.asString
              .mapError(s => Response.error(BadRequest, "auth.token:undecodeable", s.getMessage))

            body <- ZIO
              .fromEither(json.fromJson[TokenRq])
              .mapError(s => Response.error(BadRequest, "auth.token:undecodeable", s))

            // retrieving user
            user <- service
              .findByUsername(body.username)
              .catchAll(_ => ZIO succeed NotAvailable)

            response <- user match
              // user is enabled and can obtain an access token
              case Some(u) if u.status == Enabled =>

                // access granted
                if service.checkWithSalt(u.salt, body.password, u.password) then

                  // allowed applications for the user
                  val apps = u.applications.filter:
                    a => w.configuration.applications.contains(a.name)

                  for
                    // generating access and refresh tokens and making response object
                    token <- jwtHelper
                      .createToken(u.id, w.id, u.roles, apps, u.parentId)// map: t =>
                      .mapError(s => Response.error(InternalServerError, "auth.token:tokenization", s))
                      .map: t =>
                        TokenRs(
                          tokenType    = JwtHelper.TokenType,
                          accessToken  = t,
                          expiresIn    = w.configuration.auth.jwt.expiration.accessToken,
                          refreshToken = JwtHelper.createRefreshToken
                        )

                    // saving refresh token
                    _ <- refresh
                      .save(token.refreshToken, u, c.client)
                      .forkDaemon

                    // cleaning user authentication attempts
                    _ <- attempts
                      .cleanup(u)
                      .forkDaemon

                  yield Response.json(token.toJson)

                  // todo: notify event into system bus?
                  // todo: store access log

                // access denied
                else for
                  // storing login attempt
                  _ <- attempts
                    .save(u, c.client, AccessId(body.username), r.remoteAddress.map(_.toString) getOrElse "")
                    .catchAll: t =>
                      ZIO.logWarning(s"unable to save access attempt for user ${u.id}: ${t.getMessage}")

// todo:                      
//   progressive delay per identity
//   IP rate limiting
//   captcha after a lot of failed attempts
//   logging & anomaly detection
//     * IP/source
//     * device/browser fingerprint
//     * geo position
//     * velocity anomalies
//     * login from new geographical areas
//   notification
//     * SOC/monitoring
//     * ask 2FA after failed attempts
//
//                // counting total attempts
//                n <- attempts
//                  .count(user)
//                  .catchAll: t =>
//                    ZIO.logWarning(s"unable to count access attempts for user ${user.id}: ${t.getMessage}")
//                    0

                  // reached the maximum failed access attempt, blocking user
//                  _ <- ZIO
//                    .when(n + 1 >= w.configuration.auth.maxLoginAttempts):
//                      ZIO.logWarning(s"access attempt number $n for user ${body.username}")
//                        *> service.updateStatusById(user.id, Blocked) // todo: no!
                  
                  // fail: invalid credentials
//                  f <- ZIO fail InvalidCredentials
                  f <- ZIO succeed InvalidCredentials

                yield f

              case Some(u) => // user is not enabled to obtain an access token
                ZIO succeed Response.error(Forbidden, "auth.token:disabled", s"account is currently ${u.status.value.toLowerCase}")

              case _ =>
                ZIO succeed InvalidCredentials

          yield response

          val xxxx = effect
//            .catchAll(_ => ZIO succeed Response.json(res.toJson))

          xxxx
  
  val routes = Routes(authEnd, Token)

object AuthController:

  lazy val layer: ULayer[AuthController] =
    ZLayer.succeed:
      new AuthController

  sealed trait WorkspaceError(status: Status, val message: String) derives zio.json.JsonCodec
  case class MissingWorkspaceHeader(m: String) extends WorkspaceError(Status.BadRequest, m)
  case class WorkspaceNotFound(m: String) extends WorkspaceError(Status.Unauthorized, m)
  case class WorkspaceNotEnabled(m: String) extends WorkspaceError(Forbidden, m)
  case class InvalidWorkspace(m: String) extends WorkspaceError(Status.Unauthorized, m)
  case class OutOfService(m: String) extends WorkspaceError(ServiceUnavailable, m)

  def withClientCredentials(credentials: String): Task[Either[Unit, (Unit, ClientCredentials)]] =
    credentials.split(":") match
      case Array(u, p) if u == "admin" && p == "secret" => ZIO attempt Right(() -> ClientCredentials(u, p))
      case _ => ZIO fail new Throwable("Invalid credentials")


//  import io.circe.schema.Schema
//  import io.circe.parser._
//  import io.circe.generic.auto._
//
//  def validateBody[A: Decoder](schema: Schema)(handle: A => ZIO[Any, HttpError, Response]): HttpApp[Any, Nothing] =
//   Http.collectZIO[Request] {
//      case req if req.method == Method.POST =>
//        for {
//          bodyStr <- req.body.asString
//          json <- ZIO.fromEither(parse(bodyStr)).orElseFail(HttpError.BadRequest("Invalid JSON"))
//          _ <- ZIO.fromEither(schema.validate(json).toEither).orElseFail(HttpError.BadRequest("JSON does not match schema"))
//          value <- ZIO.fromEither(json.as[A]).orElseFail(HttpError.BadRequest("Invalid data"))
//          res <- handle(value)
//        } yield res
//    }
//
//    given Schema[TokenRq] = Schema.derived[TokenRq]
//      .modify(_.username):
//        _
//          .validate:
//            Validator
//              .minLength(2)
//              .and:
//                Validator.maxLength(5)
//              .and:
//                Validator.pattern("^[a-zA-Z]+$")
//      .modify(_.password):
//        _
//          .validate:
//            Validator.minLength(2)
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
import xauth.api.security.{ClientContext, ClientCredentials}
import xauth.api.controller.AbstractController
import xauth.api.controller.auth.AuthController.{InvalidWorkspace, OutOfService, WorkspaceError, WorkspaceNotEnabled}
import xauth.api.model.auth.*
import xauth.core.application.usecase.WorkspaceRegistry
import xauth.core.domain.client.port.ClientService
import zio.*
import zio.ZIO.*
import zio.http.*
import zio.http.Method.GET
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

//  import zio.http.Routes._
//  val PostToken3: (Endpoint[Unit, Unit, ZNothing, TokenRs, None], Handler[ClientHandlerEnv, Response, Request, TokenRs]) =
//    Endpoint(POST / "auth" / "token")
////      .in[TokenRq]
//      .out[TokenRs] -> (auth.ClientHandler >>> Handler.fromFunctionZIO[(ClientContext, Request)] {
//        case (c, r) => ZIO succeed res
//    })

//    val PostToken3: Route[WorkspaceRegistry & ClientService, Nothing] = {
//
//      // --- Endpoint definition ---
//      val endpoint =
//        Endpoint(POST / "auth" / "token")
//          .in[TokenRq] // <-- deserializzazione automatica
//          .out[TokenRs] // <-- serializzazione automatica
//
//      // --- Handler ---
//      val handler =
//        Handler.fromFunctionZIO[(ClientContext, Request, TokenRq)] {
//          case (ctx, req, tokenRq) =>
//
//            val res = TokenRs(
//              tokenType = "tokenType",
//              accessToken = "accessToken",
//              expiresIn = 0,
//              refreshToken = "refreshToken"
//            )
//
//            ZIO.succeed(res)
//        }
//
//      // --- Connect endpoint + handler and return a Route ---
//      endpoint.implement(auth.ClientHandler >>> handler)
//    }






//        Handler.fromFunctionZIO[TokenRq]: trq =>
//          ZIO.serviceWith[ClientResolver.CxtOut] { cc =>
////          case (cc: ClientResolver.CxtOut, r) =>
//            ZIO succeed res
//
//      @@ ClientResolver.aspect

//  val PostToken =
//    Endpoint(POST / "auth" / "token")
//      .in[TokenRq]
//      .out[TokenRs]
//////      .outError[InvalidPassword](Status.Forbidden)
//////      .outError[NotEnabledUser](Status.Forbidden)
//////      .outError[UserNotFound](Status.Unauthorized)
////      // todo: json schema validation
//      .implementAsZIO:
//        x => x
//          case (request, body, c) =>
//
//            for
////              (c, _) <- auth.ClientHandler(request)
//              _ <- ZIO.logInfo(s"w: ${c.workspace.id}, c: ${c.client.id}")
//              response = res
//            yield response

//  HandlerAspect.inter

//            ZIO.succeed(res)

//  val loginEndpoint = Endpoint(POST / "auth" / "token")
//    .in[TokenRq]
//    .out[TokenRs]
//    .outErrors(
//      HttpCodec.error[ErrorResponse](Status.BadRequest),
//      HttpCodec.error[ErrorResponse](Status.Unauthorized)
//    )
//    .implementHandler:
//      Handler.fromFunctionZIO[Request]:
//        r =>
//          for
//            _ <- auth.ClientHandler(r)
//            s <- ZIO.succeed(res)
//          yield s

  //      Handler.fromFunctionZIO[TokenRq]: body =>
  //        ZIO.serviceWithZIO[Request]: r =>
  //          for
  //            xxx <- ClientHandler(r)
  //          yield ()
  //


  val routes: Routes[WorkspaceRegistry & ClientService, Nothing] = Routes(authEnd)
  
//  val PostToken: Route[Any, Nothing] = {
//    val ep = Endpoint(POST / "auth" / "token")
//      .in[TokenRq]
//      .out[TokenRs]
//
//      
//      
//      // todo: json schema validation
//      .implementHandler:
//        Handler.fromFunctionZIO[Request]: r =>
////          for
////            _ <- auth.ClientHandler(r)
//////            (c, r2) <- auth.UserHandler(r1)
////            
//////            trq <- r.body.to[TokenRq]
////          yield ()
//
//          ZIO.succeed(res)
//          
////        Handler.fromFunctionZIO[TokenRq] { body =>
////          val res = TokenRs(
////            "tokenType",
////            "accessToken",
////            0,
////            "refreshToken"
////          )
////          ZIO.succeed(res)
////        }
//
//    ep.implementHandler {
//        Handler.fromFunctionZIO[TokenRq] { body =>
//          val res = TokenRs(
//            "tokenType",
//            "accessToken",
//            0,
//            "refreshToken"
//          )
//          ZIO.succeed(res)
//        }
//    }
//  }


//        Handler.fromFunctionZIO[(Request, TokenRq, ClientContext)] {
//          case (request, body, context) =>
//
//            given workspace: Workspace = context.workspace
//
////            Response.bo:
//              val res = TokenRs(
//                  "tokenType",
//                  "accessToken",
//                  0,
//                  "refreshToken"
//              )
//
//            import zio.schema.codec.JsonCodec
//            implicit val sss: Schema[TokenRs] = DeriveSchema.gen
//
//            def toJson[A](a: A)(using schema: Schema[A]): String = ???
////              JsonCodec.encoder(schema).encodeJson(a).toString
//
//            ZIO.succeed:
//              res
//
//        } @@ auth.ClientAspect

// Basic authentication by trusted client

//  val PostToken: Route[WorkspaceRegistry, Nothing] =
//    Endpoint(POST / "auth" / "token")
//      .in[TokenRq]
//      .out[TokenRs]
////      .outError[InvalidPassword](Status.Forbidden)
////      .outError[NotEnabledUser](Status.Forbidden)
////      .outError[UserNotFound](Status.Unauthorized)
//      // todo: json schema validation
//      .implementHandler:
//        Handler.fromFunctionZIO[(Request, TokenRq, ClientContext)] {
//          case (request, body, context) =>
//
//            given workspace: Workspace = context.workspace
//
//            // verifying current user status
//            for
//              userService <- ZIO.service[UserService]
//              jwtHelper <- ZIO.service[JwtHelper]
//
//              token <- userService
//                .findByUsername(body.username) flatMap:
//                  case Some(u) if u.status == Enabled =>
//                    // check user and password
//                    if userService.checkWithSalt(u.salt, body.password, u.password) then
//                      // todo: cleaning user authentication attempts
//                      // todo: authAccessAttemptService.deleteByUsername(body.username)
//
//                      // computing all applications currently configured in workspace
//                      val apps = u.applications.filter(a => workspace.configuration.applications.contains(a.name))
//
//                      for
//                        // creating new bearer token
//                        accessToken <- jwtHelper.createToken(u.id, context.workspace.id, u.roles, apps, u.parentId)
//                        token = TokenRs(
//                          tokenType = JwtHelper.TokenType,
//                          accessToken = accessToken,
//                          expiresIn = context.workspace.configuration.jwt.expiration.accessToken,
//                          refreshToken = JwtHelper.createRefreshToken
//                        )
//                      yield token
//
//                      // todo: saving refresh token
//                      /* todo: authRefreshTokenService.save(
//                        tokenRes.refreshToken,
//                        request.credentials.id,
//                        authUser.id
//                      )*/
//
//                      // todo: store access log
//                      // returning access token to the client
//
////                      t.map(x => Response.json(x))
////                      Response.ok.
//
//                    else { // wrong password
//                      // todo: wrong password
//                      // todo: storing login attempt
//                      Response.forbidden
//                      ZIO fail InvalidPassword("invalid user credentials")
//                    }
//
//                  case Some(u) => // not enabled user
//                    ZIO fail NotEnabledUser(s"account is currently '${u.status.value}'")
//
//                  case None => // user not found
//                    ZIO fail UserNotFound(s"invalid user credentials")
//
//              _ <- ZIO.logInfo(s"new token request from ${request.remoteAddress} for client ${context.client.id}")
//            yield token
//
//        } @@ auth.ClientAspect

object AuthController:

  lazy val layer: ULayer[AuthController] =
    ZLayer.succeed:
      new AuthController

  sealed trait WorkspaceError(status: Status, val message: String) derives zio.json.JsonCodec
  case class MissingWorkspaceHeader(m: String) extends WorkspaceError(Status.BadRequest, m)
  case class WorkspaceNotFound(m: String) extends WorkspaceError(Status.Unauthorized, m)
  case class WorkspaceNotEnabled(m: String) extends WorkspaceError(Status.Forbidden, m)
  case class InvalidWorkspace(m: String) extends WorkspaceError(Status.Unauthorized, m)
  case class OutOfService(m: String) extends WorkspaceError(Status.ServiceUnavailable, m)

  def withClientCredentials(credentials: String): Task[Either[Unit, (Unit, ClientCredentials)]] =
    credentials.split(":") match
      case Array(u, p) if u == "admin" && p == "secret" => ZIO attempt Right(() -> ClientCredentials(u, p))
      case _ => ZIO fail new Throwable("Invalid credentials")
  
//  lazy val layer: URLayer[AuthenticationManager, AuthController] =
//    ZLayer.fromZIO:
//      ZIO.service[AuthenticationManager] map:
//        new AuthController(_)


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
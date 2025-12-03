package xauth.api.auth

import xauth.api.AuthenticationManager.{AuthHandler, ClientHandler, ClientHandlerCtxOut, ClientHandlerEnv}
import xauth.api.{AbstractController, AuthenticationManager, ClientContext, ClientCredentials, ClientResolver, WorkspaceContext, WorkspaceResolver}
import xauth.api.model.auth.*
import zio.*
import zio.ZIO.*
import zio.http.*
import zio.http.Method.{GET, POST}
import zio.http.{Request, Route}
import zio.http.endpoint.Endpoint
import xauth.api.auth.AuthController.{InvalidWorkspace, OutOfService, WorkspaceError, WorkspaceNotEnabled}
import xauth.api.jwt.JwtHelper
import xauth.core.application.usecase.WorkspaceRegistry
import xauth.core.common.model.AuthStatus.Enabled
import xauth.core.domain.user.model.User
import xauth.core.domain.user.port.UserService
import xauth.core.domain.workspace.model.{Workspace, WorkspaceStatus}
import xauth.core.domain.workspace.port.WorkspaceService
import xauth.util.Uuid
import zio.http.endpoint.openapi.OpenAPI.SecurityScheme.Http
import zio.json.{JsonCodec, JsonDecoder, JsonEncoder}
import xauth.api.model.ziojson.auth.given
import xauth.core.domain.client.port.ClientService
import zio.http.Status.NotFound
import zio.http.codec.HttpCodec
import zio.schema.{DeriveSchema, Schema, derived}

//import io.circe.schema.Schema
//import io.circe.parser._
//import io.circe.generic.auto._
import io.circe._
import io.circe.generic.semiauto._

sealed class AuthController extends AbstractController:

  trait TokenError:
    val message: String

  private final case class InvalidPassword(override val message: String) extends TokenError
  private final case class NotEnabledUser(override val message: String) extends TokenError
  private final case class UserNotFound(override val message: String) extends TokenError
  
  type JsError = Json
  
  private final def err(m: String) = Json.obj("message" -> Json.fromString(m))


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

  import xauth.api.model.ziojson.info.schema
  import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec

  final case class ErrorResponse(message: String) derives JsonCodec, zio.schema.Schema

  val authEnd = POST / "auth" / "token" -> (auth.ClientHandler >>> Handler.fromFunctionZIO[(ClientContext, Request)] {
    case (c, r) =>
      ZIO succeed Response.text(s"*** w:${c.workspace.id} -> c:${c.client.id}")
  })

//  import zio.http.Routes._
//  val PostToken3 =
//    Endpoint(POST / "auth" / "token")
//      .in[TokenRq]
//      .out[TokenRs] ->
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

  val sss: Handler[WorkspaceRegistry & ClientService, Response, Request, (ClientContext, Request)] =
    auth.ClientHandler

//  val clientAspect =
//    Middleware.interceptIncomingHandler(auth.ClientHandler)

//  val autk =
//    Handler.fromFunctionZIO[(ClientContext, TokenRq)]:
//      case (_, TokenRq(u, p)) if u == "u" && p == "p" => ZIO.succeed(res)
//      case _ => ZIO.fail(ErrorResponse(message = "invalid credentials"))

  val routes: Routes[WorkspaceRegistry & ClientService, Nothing] = authEnd.toRoutes
  
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

  import xauth.api.model.ziojson.auth.given
  
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
package xauth.api.auth

import xauth.api.{AbstractController, AuthenticationManager, ClientContext, ClientCredentials, WorkspaceContext, WorkspaceResolver}
import xauth.api.model.auth.*
import zio.*
import zio.ZIO.*
import zio.http.*
import zio.http.Method.POST
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
import zio.json.{JsonDecoder, JsonEncoder}
import xauth.api.model.ziojson.auth.given

//import io.circe.schema.Schema
//import io.circe.parser._
//import io.circe.generic.auto._
import io.circe._
import io.circe.generic.semiauto._

class AuthController(auth: AuthenticationManager) extends AbstractController(auth):

  trait TokenError:
    val message: String

  private final case class InvalidPassword(override val message: String) extends TokenError
  private final case class NotEnabledUser(override val message: String) extends TokenError
  private final case class UserNotFound(override val message: String) extends TokenError
  
  type JsError = Json
  
  private final def err(m: String) = Json.obj("message" -> Json.fromString(m))

/*  val PostToken: Route[WorkspaceRegistry, Nothing] =
    Endpoint(POST / "auth" / "token")
      .in[TokenRq]
      .out[TokenRs]
//      .outError[InvalidPassword](Status.Forbidden)
//      .outError[NotEnabledUser](Status.Forbidden)
//      .outError[UserNotFound](Status.Unauthorized)
      // todo: json schema validation
      .implementHandler:
        Handler.fromFunctionZIO[(Request, TokenRq, ClientContext)] {
          case (request, body, context) =>
            
            given workspace: Workspace = context.workspace
            
            // verifying current user status
            for
              userService <- ZIO.service[UserService]
              jwtHelper <- ZIO.service[JwtHelper]
              
              token <- userService
                .findByUsername(body.username) flatMap:
                  case Some(u) if u.status == Enabled =>
                    // check user and password
                    if userService.checkWithSalt(u.salt, body.password, u.password) then
                      // todo: cleaning user authentication attempts
                      // todo: authAccessAttemptService.deleteByUsername(body.username)

                      // computing all applications currently configured in workspace
                      val apps = u.applications.filter(a => workspace.configuration.applications.contains(a.name))
                      
                      for
                        // creating new bearer token
                        accessToken <- jwtHelper.createToken(u.id, context.workspace.id, u.roles, apps, u.parentId)
                        token = TokenRs(
                          tokenType = JwtHelper.TokenType,
                          accessToken = accessToken,
                          expiresIn = context.workspace.configuration.jwt.expiration.accessToken,
                          refreshToken = JwtHelper.createRefreshToken
                        )
                      yield token
                      
                      // todo: saving refresh token
                      /* todo: authRefreshTokenService.save(
                        tokenRes.refreshToken,
                        request.credentials.id,
                        authUser.id
                      )*/

                      // todo: store access log
                      // returning access token to the client
                      
//                      t.map(x => Response.json(x))

                    else { // wrong password
                      // todo: wrong password
                      // todo: storing login attempt
                      Response.forbidden
                      ZIO fail InvalidPassword("invalid user credentials")
                    }

                  case Some(u) => // not enabled user
                    ZIO fail NotEnabledUser(s"account is currently '${u.status.value}'")

                  case None => // user not found
                    ZIO fail UserNotFound(s"invalid user credentials")
              
              _ <- ZIO.logInfo(s"new token request from ${request.remoteAddress} for client ${context.client.id}")
            yield token

        } @@ auth.ClientAspect*/

object AuthController:

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

  
  

//    val routes =
//      List(PostToken1)

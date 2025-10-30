package xauth.api

import magnolia1.Monadic.map
import xauth.api.Main.validateEnv
import xauth.api.jwt.JwtHelper
import xauth.core.common.model.AuthStatus.Enabled
import xauth.core.domain.user.port.UserService
import xauth.core.domain.workspace.model.Workspace
import xauth.util.Uuid
import zio.ZIO
import zio.http.{Handler, HandlerAspect, Request, Response}

object UserResolver:

  private val AuthHeader = "Authorization"
  private val AuthBearerRegex = "^Bearer\\s+(?<auth>.*)".r

  extension (r: Request)
    /**
     * Extracts bearer token from request [[AuthHeader]] header.
     *
     * @return Returns a [[Some[String]] object that contains
     *         the JWT bearer token if it is present into the
     *         request, returns [[None]] otherwise.
     */
    private def token: Option[String] =
      r.headers.get(AuthHeader) flatMap: s =>
        AuthBearerRegex.findFirstMatchIn(s) map:
          _.group("auth")

  private type Env = JwtHelper & WorkspaceContext & UserService 
  private type CxtOut = UserContext
  
  /** Decodes token from request header and creates a user context. */
  val aspect: HandlerAspect[Env, CxtOut] =
    HandlerAspect.interceptIncomingHandler[Env, CxtOut]:
      Handler.fromFunctionZIO[Request]: request =>
        
        for
          jwtHelper <- ZIO.service[JwtHelper]
          wsCtx <- ZIO.service[WorkspaceContext]
          userService <- ZIO.service[UserService]

          // retrieving token from http header
          token <- ZIO
            .fromOption(request.token)
            .orElseFail(Response unauthorized "authentication token not found in request header")
            .mapError(_ => Response.internalServerError("unable to extract token from request header"))

          // decoding token
          (userId, workspaceId, _) <- jwtHelper
            .decodeToken(token)(using wsCtx.workspace)
            .mapError(Response.unauthorized)

          // user retrieval
          context <-
            if wsCtx.workspace.id == workspaceId then
              userService
                .findById(userId)(using wsCtx.workspace)
                .mapError(t => Response.internalServerError(t.getMessage))
                .flatMap:
                  case Some(u) if u.status == Enabled =>
                    ZIO succeed new UserContext(wsCtx.workspace, u)
                  case Some(u) =>
                    ZIO.logWarning(s"invalid access token for user $userId from ${request.remoteAddress}") *>
                      ZIO.fail(Response.forbidden(s"account is currently '${u.status}'"))
                  case None =>
                    ZIO.fail(Response.unauthorized("invalid access token"))

            else ZIO.fail(Response.unauthorized("inconsistent workspace request"))

        yield (request, context)
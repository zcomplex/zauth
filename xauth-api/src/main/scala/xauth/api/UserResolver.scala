package xauth.api

import xauth.api.jwt.JwtHelper
import xauth.core.common.model.AuthStatus.Enabled
import xauth.core.domain.user.port.UserService
import zio.ZIO
import zio.http.{Handler, Request, Response}

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

  type Env = JwtHelper & WorkspaceContext & UserService 
  type CxtOut = UserContext
  
  private type CxtIn = WorkspaceResolver.CxtOut
  
  private type UserHandler = Handler[Env, Response, (CxtIn, Request), (CxtOut, Request)]

  /** Decodes token from request header and creates a user context. */
  val handler: UserHandler = 
    Handler.fromFunctionZIO[(CxtIn, Request)]:
      case (i, r) =>
        for
          jwtHelper <- ZIO.service[JwtHelper]
          userService <- ZIO.service[UserService]
  
          // retrieving token from http header
          token <- ZIO
            .fromOption(r.token)
            .orElseFail(Response unauthorized "authentication token not found in request header")
            .mapError(_ => Response.internalServerError("unable to extract token from request header"))
  
          // decoding token
          (userId, workspaceId, _) <- jwtHelper
            .decodeToken(token)(using i.workspace)
            .mapError(Response.unauthorized)
  
          // user retrieval
          context <-
            if i.workspace.id == workspaceId then
              userService
                .findById(userId)(using i.workspace)
                .mapError(t => Response.internalServerError(t.getMessage))
                .flatMap:
                  case Some(u) if u.status == Enabled =>
                    ZIO succeed new UserContext(i.workspace, u)
                  case Some(u) =>
                    ZIO.logWarning(s"invalid access token for user $userId from ${r.remoteAddress}") *>
                      ZIO.fail(Response.forbidden(s"account is currently '${u.status}'"))
                  case None =>
                    ZIO.fail(Response.unauthorized("invalid access token"))
  
            else ZIO.fail(Response.unauthorized("inconsistent workspace request"))
  
        yield (context, r)
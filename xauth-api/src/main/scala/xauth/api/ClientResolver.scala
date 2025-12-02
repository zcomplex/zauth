package xauth.api

import xauth.core.domain.client.port.ClientService
import xauth.util.ext.md5
import zio.ZIO
import zio.http.*

import java.util.Base64

/**
 * Implements logic to perform a basic authentication.
 */
object ClientResolver:

  private val AuthHeader = "Authorization"
  private val AuthBasicRegex = "^Basic\\s+(?<auth>.*)".r

  extension (r: Request)
    /**
     * Extracts client credentials from request [[AuthHeader]] header.
     *
     * @return Returns a ClientCredentials object that contains the client credentials.
     */
    private def clientCredentials: Option[ClientCredentials] =
      r.headers.get(AuthHeader) flatMap: v =>
        AuthBasicRegex.findFirstMatchIn(v) map: m =>
          val auth = m.group("auth")
          val ss = new String(Base64.getDecoder.decode(auth)).split(":", 2)
          ClientCredentials(ss(0), ss(1))

  type Env = ClientService
  type CxtOut = ClientContext

  private type CxtIn = WorkspaceResolver.CxtOut

  private type ClientHandler = Handler[Env, Response, (CxtIn, Request), (CxtOut, Request)]

  val handler: ClientHandler =
    Handler.fromFunctionZIO[(WorkspaceContext, Request)]:
      case (w, r) =>
        for
          clientService <- ZIO.service[ClientService]

          // extracting client credentials from request header
          cc <- ZIO
            .fromOption(r.clientCredentials)
            .orElseFail:
              Response forbidden "client credentials are required"

          // checking credentials and creating the client context
          context <- clientService
            .find(cc.id)(using w.workspace)
            .mapError(t => Response.internalServerError(t.getMessage))
            .flatMap:
              case Some(c) if c.id == cc.id && c.secret.md5 == cc.secret =>
                ZIO succeed new ClientContext(w.workspace, c)
              case Some(_) =>
                ZIO.logWarning(s"invalid client credentials for ${cc.id}:${cc.secret}") *>
                  ZIO.fail(Response forbidden s"invalid client credentials")
              case _ =>
                ZIO.logWarning(s"bad client credentials for ${cc.id}:${cc.secret}") *>
                  ZIO.fail(Response unauthorized "bad client credentials")
        yield (context, r)
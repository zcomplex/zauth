package xauth.api

import xauth.api.Main.validateEnv
import xauth.core.domain.client.port.ClientService
import xauth.util.ext.md5
import zio.ZIO
import zio.http.*
import zio.http.Header.Authorization

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
  
  private type CtxOut = ClientContext
  
  val aspect: HandlerAspect[WorkspaceContext & ClientService, CtxOut] = ???
//    HandlerAspect.interceptIncomingHandler[ClientService, CtxOut]:
//      Handler.fromFunctionZIO[Request]: request =>
//        for
//          wsCtx <- ZIO.service[WorkspaceContext]
//          clientService <- ZIO.service[ClientService]
//
//          // extracting client credentials from request header
//          cc <- ZIO
//            .fromOption(request.clientCredentials)
//            .orElseFail:
//              Response forbidden "client credentials are required"
//
//          // checking credentials and creating the client context
//          context <- clientService
//            .find(cc.id)(using wsCtx.workspace)
//            .mapError(t => Response.internalServerError(t.getMessage))
//            .flatMap:
//              case Some(c) if c.id == cc.id && c.secret.md5 == cc.secret =>
//                ZIO succeed new ClientContext(wsCtx.workspace, c)
//              case Some(_) =>
//                ZIO.logWarning(s"invalid client credentials for ${cc.id}:${cc.secret}") *>
//                  ZIO.fail(Response forbidden s"invalid client credentials")
//              case _ =>
//                ZIO.logWarning(s"bad client credentials for ${cc.id}:${cc.secret}") *>
//                  ZIO.fail(Response unauthorized "bad client credentials")
//
//        yield (request, context)
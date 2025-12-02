package xauth.api

import xauth.core.application.usecase.WorkspaceRegistry
import xauth.core.domain.workspace.model.WorkspaceStatus
import xauth.util.Uuid
import zio.ZIO
import zio.http.{Handler, Request, Response}

/**
 * Implements logic to resolve workspace through the `X-Workspace-Id` http header in request.
 */
object WorkspaceResolver:
  private val WorkspaceHeader = "X-Workspace-Id"
  private val UuidRegex = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$".r

  extension (r: Request)
    /** Extracts workspace id from request X-Workspace-Id header. */
    private def workspaceId: Option[Uuid] =
      r.headers.get(WorkspaceHeader) flatMap: v =>
        UuidRegex.findFirstMatchIn(v) map (m => Uuid(m.group(0)))
        
  type Env = WorkspaceRegistry
  type CxtOut = WorkspaceContext

  type WorkspaceHandler = Handler[Env, Response, Request, (CxtOut, Request)]

  /** Retrieves the workspace by its identifier in http header and creates the workspace context. */
  val handler: WorkspaceHandler =
    Handler.fromFunctionZIO[Request]: request =>
      for
        registry <- ZIO.service[WorkspaceRegistry]
        wId <- ZIO
          .fromOption(request.workspaceId)
          .mapError:
            _ => Response unauthorized s"missing $WorkspaceHeader header in request"
        context <- registry
          .workspace(wId)
          .flatMap:
            case Some(w) if w.status == WorkspaceStatus.Enabled =>
              ZIO succeed new WorkspaceContext(w)
            case Some(w) =>
              ZIO fail Response.forbidden(s"workspace is currently '${w.status}'")
            case None =>
              ZIO fail Response.unauthorized("invalid workspace id")
      yield (context, request)
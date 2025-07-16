package xauth.infrastructure.mongo

import xauth.util.mongo.WorkspaceCollection as WCollection

/** Defines workspace persistence collections. */
enum WorkspaceCollection(name: String) extends WCollection(name):
  case AccessAttempt extends WorkspaceCollection("w_access_attempt")
  case Client        extends WorkspaceCollection("w_client")
  case Code          extends WorkspaceCollection("w_code")
  case RefreshToken  extends WorkspaceCollection("w_refresh_token")
  case User          extends WorkspaceCollection("w_user")
  case Invitation    extends WorkspaceCollection("w_invitation")
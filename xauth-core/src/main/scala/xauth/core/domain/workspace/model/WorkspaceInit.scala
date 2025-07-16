package xauth.core.domain.workspace.model

/** Workspace initialization. */
case class WorkspaceInit(client: Client, admin: Admin)

/** Initial client for http-basic authentication. */
case class Client(id: String, secret: String)

/** Initial workspace administrator. */
case class Admin(username: String, password: String)
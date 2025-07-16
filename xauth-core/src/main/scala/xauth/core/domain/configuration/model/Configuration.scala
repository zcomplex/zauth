package xauth.core.domain.configuration.model

import xauth.core.domain.workspace.model.WorkspaceConfiguration

case class Configuration
(
  baseUrl: String,
  confPath: String,
  init: InitConfiguration
)

case class InitConfiguration
(
  admin: UserConfiguration,
  client: ClientConfiguration,
  workspace: WorkspaceConfiguration
)
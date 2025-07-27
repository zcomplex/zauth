package xauth.core.domain.configuration.model

import xauth.core.domain.user.model.UserInfo
import xauth.core.domain.workspace.model.WorkspaceConf

case class Configuration
(
  baseUrl: String,
  confPath: String,
  init: InitConfiguration
)

case class InitConfiguration
(
  admin: UserConf,
  client: ClientConf,
  workspace: WorkspaceConf
)

case class UserConf
(
  username: String,
  password: String,
  info: UserInfo
)

case class ClientConf(id: String, secret: String)

case class WorkspaceConf
(
  slug: String,
  description: String,
  company: CompanyConf,
  configuration: xauth.core.domain.workspace.model.WorkspaceConf
)

case class CompanyConf(name: String, description: String)
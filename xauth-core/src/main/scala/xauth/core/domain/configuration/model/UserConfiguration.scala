package xauth.core.domain.configuration.model

import xauth.core.domain.user.model.UserInfo

case class UserConfiguration
(
  username: String,
  password: String,
  info: UserInfo
)

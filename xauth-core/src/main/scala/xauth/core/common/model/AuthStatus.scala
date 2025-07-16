package xauth.core.common.model

import xauth.util.{EnumFromVal, EnumVal}

/** Defines all recognized and handled user statuses. */
enum AuthStatus(val value: String) extends EnumVal[String]:

  /** Defines the disabled status. */
  case Disabled extends AuthStatus("DISABLED")

  /** Defines the enabled status for the working user. */
  case Enabled  extends AuthStatus("ENABLED")

  /** Defines the status for the blocked user. */
  case Blocked  extends AuthStatus("BLOCKED")

object AuthStatus extends EnumFromVal[AuthStatus, String]
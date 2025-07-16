package xauth.core.common.model

import xauth.util.{EnumFromVal, EnumVal}

/** Defines all recognized and handled user roles. */
enum AuthRole(val value: String) extends EnumVal[String]:

  /** Defines simple user role. */
  case User             extends AuthRole("USER")

  /** Defines the human resource role. */
  case HumanResource    extends AuthRole("HR")

  /** Defines the help desk operator role. */
  case HelpDeskOperator extends AuthRole("HD_OPERATOR")

  /** Defines the application responsible role. */
  case Responsible      extends AuthRole("RESPONSIBLE")

  /** Defines the administrator role. */
  case Admin            extends AuthRole("ADMIN")

  /** Defines the system root administrator role. */
  case System           extends AuthRole("SYSTEM")

object AuthRole extends EnumFromVal[AuthRole, String]
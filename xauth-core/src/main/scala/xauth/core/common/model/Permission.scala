package xauth.core.common.model

import xauth.util.{EnumFromVal, EnumVal}

/** Defines all recognized and handled permission types. */
enum Permission(val value: String) extends EnumVal[String]:
  
  case Owner     extends Permission("OWNER")
  case Read      extends Permission("READ")
  case Write     extends Permission("WRITE")
  case Execution extends Permission("EXECUTION")

object Permission extends EnumFromVal[Permission, String]
package xauth.core.domain.workspace.model

import xauth.util.{EnumFromVal, EnumVal}

/** Defines workspace status. */
enum WorkspaceStatus(val value: String) extends EnumVal[String]:

  /** Defines the enabled status. */
  case Enabled extends WorkspaceStatus("ENABLED")

  /** Defines the disabled status. */
  case Disable extends WorkspaceStatus("DISABLED")
  
object WorkspaceStatus extends EnumFromVal[WorkspaceStatus, String]
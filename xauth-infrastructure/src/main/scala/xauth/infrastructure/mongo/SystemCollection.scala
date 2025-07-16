package xauth.infrastructure.mongo

import xauth.util.mongo.SystemCollection as SCollection

/** Defines all basic collections. */
enum SystemCollection(name: String) extends SCollection(name):
  case Setting   extends SystemCollection("s_setting")
  case Tenant    extends SystemCollection("s_tenant")
  case Workspace extends SystemCollection("s_workspace")
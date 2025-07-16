package xauth.core.common.model

import xauth.util.{EnumFromVal, EnumVal}

/** Defines all recognized and handled contact types. */
enum ContactType(val value: String) extends EnumVal[String]:
  case Email        extends ContactType("EMAIL")
  case MobileNumber extends ContactType("MOBILE_NUMBER")
  
object ContactType extends EnumFromVal[ContactType, String]
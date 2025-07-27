package xauth.core.domain.code.model

import xauth.util.{EnumFromVal, EnumVal}

/** Defines authentication code types. */
enum AccountCodeType(val value: String) extends EnumVal[String]:

  /** Defines the type for account activation code. */
  case Activation extends AccountCodeType("ACTIVATION")

  /** Defines the type for account deletion code. */
  case Deletion extends AccountCodeType("DELETION")

  /** Defines the type for user contact trust code. */
  case ContactTrust extends AccountCodeType("CONTACT_TRUST")

  /** Defines the type for password reset code. */
  case PasswordReset extends AccountCodeType("PASSWORD_RESET")

  /** Defines the type for invitation registration code. */
  case Invitation extends AccountCodeType("INVITATION")

object AccountCodeType extends EnumFromVal[AccountCodeType, String]
/*
 * Copyright (C) 2025-Present ZAuth.
 * This file is part of ZAuth, Multi-Tenant Authentication System.
 *
 * This software is released under the ZAuth License V1, which is based on the
 * GNU General Public License version 3 (GPLv3) as published by the Free Software
 * Foundation, with an additional "No SaaS" clause.
 *
 * You may redistribute and/or modify it under the terms of the GPLv3 as
 * published by the Free Software Foundation, with the added restriction that
 * this software may not be provided as a public network service (SaaS,
 * DBaaS, API, or similar) without prior written authorization from the author.
 *
 * THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT PERMITTED BY
 * APPLICABLE LAW. EXCEPT WHEN OTHERWISE STATED IN WRITING THE COPYRIGHT
 * HOLDERS AND/OR OTHER PARTIES PROVIDE THE PROGRAM "AS IS" WITHOUT WARRANTY
 * OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE. THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE PROGRAM
 * IS WITH YOU. SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF
 * ALL NECESSARY SERVICING, REPAIR OR CORRECTION.
 *
 * This software is released under ZAuth License V1.
 * See LICENSE for full terms.
 */
package xauth.core.common.model

import xauth.util.{EnumFromVal, EnumVal}

/** Defines all possible authentication types. */
enum AuthType(val value: String) extends EnumVal[String]:

  /** Classic: E-mail. */
  case Email       extends AuthType("EMAIL")
  /** Classic: Mobile number. */
  case Mobile      extends AuthType("MOBILE")
  /** Classic: Phone number. */
  case Phone       extends AuthType("PHONE")
  /** Classic: Username. */
  case Username    extends AuthType("USERNAME")

  /** Certificates and keys: Client certificate. */
  case Client      extends AuthType("CERTIFICATE")
  /** Certificates and keys: Passkey (WebAuthn/FIDO2). */
  case Passkey     extends AuthType("PASSKEY")
  /** Certificates and keys: Public keys (SSH, PGP, X.509, smart card, proprietary protocols, ...). */
  case PublicKey   extends AuthType("PUBKEY")

  /** Biometrics: Face recognition. */
  case Face        extends AuthType("FACE")
  /** Biometrics: Fingerprint. */
  case Fingerprint extends AuthType("FINGERPRINT")
  /** Biometrics: Iris scan. */
  case IrisScan    extends AuthType("IRIS")
  /** Biometrics: Voice print. */
  case VoicePrint  extends AuthType("VOICE")

  /** Context-based: Device identifier. */
  case Device      extends AuthType("DEVICE")
  /** Context-based: Magic link. */
  case Link        extends AuthType("LINK")

object AuthType extends EnumFromVal[AuthType, String]
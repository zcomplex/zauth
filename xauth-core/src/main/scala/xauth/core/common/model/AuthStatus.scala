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

/**
 * Defines all recognized and handled user statuses.
 *
 * todo: the user.status field will reflect the last updated status,
 *       for instance if a user account is attacked and after a while
 *       it is going to expire (expiring status), the user.status
 *       field will reflect the expiration but the cumulative bitmask
 *       into the access log registry will represent the real status,
 *       for instance to represent both the attacked and expiring statuses.
 *
 * todo: create a bitmask field to represent the multi-state.
 *
 * todo: split and distinct statuses from flags:
 *       - status: [new, enabled, blocked, expired, removed, deleted, disabled]
 *       - flags: [attacked, attacking, expiring, removing]
 */
enum AuthStatus(val value: String) extends EnumVal[String]:

  /**
   * A past or recent attack has been detected and mitigated.
   * No immediate threat is active, but the account is flagged
   * for monitoring or stepped-up authentication.
   *
   * Account needs attention, user acknowledge is needed and have to
   * read access log registry.
   *
   * Possible transitions:
   * <pre>
   *   -> attacking - attack started again.
   *   -> blocked
   *   -> deleted
   *   -> disabled
   *   -> enabled   - security activities and acknowledgements complete.
   *   -> expiring
   *   -> removed
   * </pre>
   */
  case Attacked extends AuthStatus("ATTACKED")

  /**
   * The account is currently under an active brute-force or credential-stuffing attack.
   * This state is non-blocking for the legitimate owner.
   *
   * Security controls such as rate-limits, MFA escalation, proof-of-work,
   * temporary IP throttling are applied.
   *
   * Possible transitions:
   * <pre>
   *   -> attacked - attack finished.
   *   -> blocked
   *   -> deleted
   *   -> disabled
   *   -> expiring
   *   -> removed
   * </pre>
   */
  case Attacking extends AuthStatus("ATTACKING")

  /**
   * The account has been created but not yet verified.
   *
   * Possible transitions:
   * <pre>
   *   -> enabled  - after activation (at least 1 trusted contact)
   *   -> removing - before the physical deletion
   * </pre>
   */
  case New extends AuthStatus("NEW")

  /**
   * The account is registered and verified, the user is active.
   *
   * Possible transitions:
   * <pre>
   *   -> attacked
   *   -> attacking
   *   -> blocked
   *   -> deleted
   *   -> disabled
   *   -> expired
   *   -> expiring
   *   -> removed
   * </pre>
   */
  case Enabled extends AuthStatus("ENABLED")

  /**
   * Administrative or automated security mechanism has fully blocked access to the account
   * due to confirmed malicious activity originating from the account owner
   * (e.g., policy violation, confirmed compromise).
   *
   * The user can delete its account but its ban will be stored to avoid bad activities with
   * next further new accounts.
   *
   * Possible transitions:
   * <pre>
   *   -> deleted
   *   -> disabled
   *   -> enabled
   *   -> expired
   *   -> expiring
   *   -> removed
   * </pre>
   */
  case Blocked extends AuthStatus("BLOCKED")

  /**
   * The account has reached its expiration date or inactivity threshold and cannot be used for authentication.
   *
   * Possible transitions:
   * <pre>
   *   -> enabled - administrators can renew the account
   *   -> deleted
   *   -> removed
   * </pre>
   */
  case Expired extends AuthStatus("EXPIRED")

  /**
   * The account is approaching expiration, e.g. contract ending, inactivity threshold nearly reached.
   * User receives notifications or warnings.
   *
   * Transitions to attacked, attacking or other states are possible but the account will still go
   * to the expiration if no renewal event will happen after this state.
   *
   * Possible transitions:
   * <pre>
   *   -> attacked
   *   -> attacking
   *   -> blocked
   *   -> deleted
   *   -> disabled
   *   -> expired
   *   -> removed
   * </pre>
   */
  case Expiring extends AuthStatus("EXPIRING")

  /**
   * The account has been removed by the user.
   * All sensible data will be hashed with a one-way algorithm.
   *
   * Final status, not recoverable.
   */
  case Deleted extends AuthStatus("DELETED")

  /**
   * The account has been removed by an administrator.
   * All sensible data will be hashed with a one-way algorithm.
   *
   * Final status, not recoverable.
   */
  case Removed extends AuthStatus("REMOVED")

  /**
   * The account will be automatically removed by the system.
   *
   * Possible transitions:
   * <pre>
   *   -> attacked
   *   -> attacking
   *   -> blocked
   *   -> deleted
   *   -> disabled
   *   -> expired
   *   -> enabled
   *   -> expiring
   *   -> removed
   * </pre>
   */
  case Removing extends AuthStatus("REMOVING")

  /**
   * The account has been temporarily or permanently disabled by an administrator or the system.
   * Used to prevent login or actions without deleting user data.
   *
   * Possible transitions:
   * <pre>
   *   -> enabled
   *   -> blocked
   *   -> deleted
   *   -> disabled
   *   -> removed
   * </pre>
   */
  case Disabled extends AuthStatus("DISABLED")

object AuthStatus extends EnumFromVal[AuthStatus, String]
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
package xauth.api.jwt

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.KeyUse.SIGNATURE
import com.nimbusds.jose.jwk.{JWK, RSAKey}
import io.circe.Json
import io.circe.syntax.*
import pdi.jwt.algorithms.{JwtAsymmetricAlgorithm, JwtHmacAlgorithm}
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim, JwtHeader}
import xauth.api.jwt.JwtHelper.*
import xauth.api.jwt.JwtHelper.AlgorithmType.{Asymmetric, Symmetric}
import xauth.core.common.model.AuthRole
import xauth.core.domain.configuration.model.Configuration
import xauth.core.domain.user.model.AppInfo
import xauth.core.domain.workspace.model.Workspace
import xauth.infrastructure.user.json.given
import xauth.util.Uuid
import xauth.util.ext.{bytes, toPrivateKey, toPublicKey, toSecretKey}
import zio.{IO, URLayer, ZIO, ZLayer}

import java.io.File
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import scala.util.Try

/**
 * todo: trait and default implementation
 */
final class JwtHelper(conf: Configuration):

  def createToken(userId: Uuid, workspaceId: Uuid, roles: Seq[AuthRole] = Nil, applications: Seq[AppInfo] = Nil, parentId: Option[Uuid])(using w: Workspace): IO[String, AccessToken] =
    ZIO.succeed:
      val seq = Seq(
        Some("workspaceId" -> workspaceId.stringValue),
        Some("roles" -> roles),

        parentId map:
          pid => "parentId" -> pid.stringValue,

        Option.when(applications.nonEmpty):
          "applications" -> applications.asJson
      )

      val data = seq.flatten

      val timestamp = Instant.now.getEpochSecond

      val claim = JwtClaim()
        .by(w.company.name)
        .issuedAt(timestamp)
        .expiresAt(timestamp + w.configuration.auth.jwt.expiration.accessToken)
        .about(userId.stringValue).++(data*)
  
      // reading algorithm from workspace configuration
      val wAlg = w.configuration.auth.jwt.encryption.algorithm
      val algorithm: JwtAlgorithm = JwtAlgorithm.fromString(wAlg)
      val aType = toAlgorithmType(wAlg)
  
      aType match
        case Asymmetric =>
          // signing token with private key
          val key = asymmetricKey
          val header = JwtHeader(
            algorithm = Some(algorithm),
            typ = Some("JWT"),
            keyId = Some(key.name)
          )
          Jwt.encode(header, claim, key.privateKeyBytes.toPrivateKey)
        case Symmetric =>
          // signing token with secret key
          val key = symmetricKey
          Jwt.encode(claim, key.bytes.toSecretKey, algorithm.asInstanceOf[JwtHmacAlgorithm])

  def decodeToken(token: String)(using w: Workspace): IO[String, DecodedTokenInfo] =
    for
      decoded <- ZIO.succeed:
        // reading algorithm from workspace configuration
        val wAlg = w.configuration.auth.jwt.encryption.algorithm
        val algorithm: JwtAlgorithm = JwtAlgorithm.fromString(wAlg)

        toAlgorithmType(wAlg) match
          case Asymmetric =>
            val key = asymmetricKey
            val publicKey = key.publicKeyBytes.toPublicKey
            Jwt.decodeRawAll(token, publicKey, Seq(algorithm.asInstanceOf[JwtAsymmetricAlgorithm]))
          case Symmetric =>
            val secretKey = symmetricKey.bytes.toSecretKey
            Jwt.decodeRawAll(token, secretKey, Seq(algorithm.asInstanceOf[JwtHmacAlgorithm]))

      info <- ZIO
        .fromTry(decoded)
        .map: (_, c, _) =>
          // handling decoding result
          val json = Json.fromString(c)
          (
            json.hcursor.get[String]("sub").map(Uuid.apply) getOrElse Uuid.Zero,
            json.hcursor.get[String]("workspaceId").map(Uuid.apply) getOrElse Uuid.Zero,
            json.hcursor.get[List[String]]("roles").map(r => r.map(AuthRole.fromValue)) getOrElse Nil
          )
        .mapError(_.getMessage)

    yield info

  def jwk(using w: Workspace): Option[JWK] =
    val wAlg = w.configuration.auth.jwt.encryption.algorithm
    toAlgorithmType(wAlg) match
      case Asymmetric => Some:
        val algorithm = JWSAlgorithm.parse(wAlg)
        val key = asymmetricKey
        new RSAKey.Builder(key.publicKeyBytes.toPublicKey.asInstanceOf[RSAPublicKey])
          .privateKey(key.privateKeyBytes.toPrivateKey)
          .keyUse(SIGNATURE)
          .keyID(s"${w.slug}-default")
          .algorithm(algorithm)
          .build
      case Symmetric => None

  private def asymmetricKey(implicit w: Workspace): AsymmetricKey =
    val path = s"${conf.confPath}/keys"
    val pvtBytes = new File(path, s"${w.id.stringValue}/${w.id}-rsa.private.der").bytes
    val pubBytes = new File(path, s"${w.id.stringValue}/${w.id}-rsa.public.der").bytes
    (w.id.stringValue, pvtBytes, pubBytes)

  private def symmetricKey(implicit w: Workspace): SymmetricKey =
    val path = s"${conf.confPath}/keys"
    val key = new File(path, s"${w.id.stringValue}/${w.id}-rsa.private.der")
    (w.id.stringValue, key.bytes)

object JwtHelper:

  val TokenType: String = "bearer"
  
  enum AlgorithmType:
    case Symmetric, Asymmetric
    
  private type SymmetricKey = (name: String, bytes: Array[Byte])
  private type AsymmetricKey = (name: String, privateKeyBytes: Array[Byte], publicKeyBytes: Array[Byte])
  
  type AccessToken = String
  type DecodedTokenInfo = (sub: Uuid, workspaceId: Uuid, roles: Seq[AuthRole])

  def isAsymmetricAlgorithm(s: String): Boolean =
    val a = JwtAlgorithm.fromString(s)
    JwtAlgorithm.allAsymmetric().contains(a)
  
  def toAlgorithmType(algorithm: String): AlgorithmType =
    val a = JwtAlgorithm.fromString(algorithm)
    if (JwtAlgorithm.allAsymmetric().contains(a)) Asymmetric else Symmetric

  def createRefreshToken: String =
    import xauth.util.ext.random
    (('a' to 'f') ++ ('0' to '9')).random(40)

  lazy val layer: URLayer[Configuration, JwtHelper] = ZLayer.fromZIO:
    ZIO.service[Configuration] map:
      new JwtHelper(_)
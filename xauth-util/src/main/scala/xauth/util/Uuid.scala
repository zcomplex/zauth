package xauth.util

import xauth.util.UuidType.Anonymous

import java.nio.ByteBuffer
import java.util.UUID

case class Uuid(uuid: Option[UUID] = None):

  val value: UUID = uuid match
    case Some(s) => s
    case None => UUID.randomUUID

  val idType: UuidType = Anonymous // todo: handle it!

  val stringValue: String = value.toString

  // uuid(<id-type>, db9d9c40-e2eb-43de-910d-6a24114f11c5)
  override def toString: String = s"uuid(${idType.value.toLowerCase}, $stringValue)"

object Uuid:

  val Zero: Uuid = Uuid("00000000-0000-0000-0000-000000000000")

  def apply(uuid: String): Uuid = new Uuid(Some(UUID.fromString(uuid)))

  def apply(bytes: Array[Byte]): Uuid =
    new Uuid(Some(new UUID(
      ByteBuffer.wrap(bytes.slice(0, 8)).getLong,
      ByteBuffer.wrap(bytes.slice(8, 16)).getLong
    )))

enum UuidType(val value: String) extends EnumVal[String]:

  /** Defines an anonymous uuid type. */
  case Anonymous  extends UuidType("ANONYMOUS")

  /** Defines the uuid type for tenant type. */
  case Tenant     extends UuidType("TENANT")

  /** Defines the uuid type for workspace type. */
  case Workspace  extends UuidType("WORKSPACE")

  /** Defines the uuid type for invitation type. */
  case Invitation extends UuidType("INVITATION")

  /** Defines the uuid type for user type. */
  case User       extends UuidType("USER")

object UuidType extends EnumFromVal[UuidType, String]
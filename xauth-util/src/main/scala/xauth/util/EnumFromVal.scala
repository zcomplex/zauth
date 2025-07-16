package xauth.util

trait EnumFromVal[E <: EnumVal[A], A]:
  def values: Array[E]

  def fromValue(value: A): E =
    fromValueOpt(value) match
      case Some(v) => v
      case None => throw new IllegalArgumentException(s"illegal enum value: $value")

  def fromValueOpt(value: A): Option[E] =
    values.find(_.value == value)
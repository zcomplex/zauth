package xauth.util

import zio.UIO
import zio.stm.TMap

/**
 * Represents a multi-tenant registry.
 * @param data The data map.
 * @tparam I Identifier.
 * @tparam A The value type.
 */
abstract class Registry[I, A](data: TMap[I, A]):
  
  def get(id: I): UIO[Option[A]] =
    data
      .get(id)
      .commit
  
  def put(id: I, v: A): UIO[Unit] =
    data
      .put(id, v)
      .commit

  def delete(id: I): UIO[Unit] =
    data
      .delete(id)
      .commit
  
  def size: UIO[Int] =
    data
      .size
      .commit
package xauth.core.domain.system.port

import xauth.core.domain.system.model.{SystemSetting, SettingKey}
import xauth.core.spi.Repository
import zio.Task

import scala.reflect.ClassTag

/** Allow to read and write system settings. */
trait SystemSettingRepository extends Repository[SystemSetting, SettingKey]:

  /** Saves the setting for the given key and value. */
  def save[A](k: SettingKey, v: A): Task[A]

  /** Finds the setting value for the given key and returns a parsed value. */
  def read[A](k: SettingKey)(using ev: ClassTag[A]): Task[Option[A]] =
    find(k) map: o =>
      o map: x =>
        val v = x.value
        val parsed = ev.runtimeClass match
          case b if b == classOf[Boolean] => v.toBoolean
          case i if i == classOf[Int]     => v.toInt
          case l if l == classOf[Long]    => v.toLong
          case d if d == classOf[Double]  => v.toDouble
          case f if f == classOf[Float]   => v.toFloat
          case s if s == classOf[String]  => v

          case _ => throw new IllegalArgumentException(s"Unsupported type: ${ev.runtimeClass}")

        parsed.asInstanceOf[A]
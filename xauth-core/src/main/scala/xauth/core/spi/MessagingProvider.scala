package xauth.core.spi

import xauth.core.domain.workspace.model.{ProviderConf, Workspace}
import xauth.core.spi.MessagingProvider.setting
import zio.UIO

trait MessagingProvider:

  val conf: ProviderConf

  protected def setting[A](k: String): Option[A] =
    conf.setting(k)

object MessagingProvider:

  trait ProviderRegistry:
    /** Creates and returns all active messaging providers for the given workspace. */
    def providers(using w: Workspace): UIO[Set[MessagingProvider]]

  extension (conf: ProviderConf)
    def setting[A](k: String): A = settingOpt(k).get
    def setting[A](k: String, d: A): A = settingOpt(k) getOrElse d
    def settingOpt[A](k: String): Option[A] =
      conf.configuration.get(k) map:
        _.asInstanceOf[A]
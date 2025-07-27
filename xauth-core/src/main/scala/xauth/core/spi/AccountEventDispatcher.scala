package xauth.core.spi

import xauth.core.domain.user.model.User
import xauth.core.domain.workspace.model.Workspace
import zio.UIO

sealed trait AccountEvent
object AccountEvent:
  final case class UserRegistered(u: User, w: Workspace) extends AccountEvent

trait AccountEventDispatcher:
  def dispatch(event: AccountEvent): UIO[Unit]
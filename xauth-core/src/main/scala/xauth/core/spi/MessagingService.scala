package xauth.core.spi

import xauth.core.domain.code.model.AccountCode
import xauth.core.domain.user.model.User
import xauth.core.domain.workspace.model.Workspace
import zio.UIO

/** Responsible to prepare messages for send. */
trait MessagingService:

  /** Notifies user registration to the listeners. */
  def notifyUserRegistration(u: User)(using w: Workspace): UIO[Unit]

  /** Sends activation message with the activation instructions. */
  def sendActivationMessage(u: User, c: AccountCode)(using w: Workspace): UIO[Unit]
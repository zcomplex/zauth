package xauth.core.application.usecase

import xauth.core.domain.user.model.User
import xauth.core.domain.workspace.model.Workspace
import xauth.core.spi.MessagingService
import zio.{UIO, ZIO}

class MessagingServiceImpl extends MessagingService:

  /** Notifies user registration to the listeners. */
  override def notifyUserRegistration(u: User)(using w: Workspace): UIO[Unit] =
    ZIO logInfo s"mock: dispatching new-user notifications"

  /** Sends activation message with the activation instructions. */
  override def sendActivationMessage(u: User)(using w: Workspace): UIO[Unit] =
    ZIO logInfo s"mock: dispatching activation message"

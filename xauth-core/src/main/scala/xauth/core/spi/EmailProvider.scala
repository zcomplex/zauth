package xauth.core.spi

import zio.UIO

trait EmailProvider extends MessagingProvider:
  def sendText(to: String, subject: String, message: String): UIO[Unit]
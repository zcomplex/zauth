package xauth.core.spi

import zio.UIO

trait SmsProvider extends MessagingProvider:
  def send(to: String, message: String): UIO[Unit]
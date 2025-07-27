package xauth.infrastructure.messaging.provider.email

import org.apache.commons.mail.SimpleEmail
import xauth.core.domain.workspace.model.ProviderConf
import xauth.core.spi.EmailProvider
import xauth.core.spi.MessagingProvider.setting
import xauth.infrastructure.messaging.provider.email.QueuedEmailProvider.EmailJob
import zio.*

final class QueuedEmailProvider(override val conf: ProviderConf, queue: Queue[EmailJob]) extends EmailProvider:

  override def sendText(to: String, subject: String, message: String): UIO[Unit] =
    queue.offer((to, subject, message)).unit

object QueuedEmailProvider:

  val Name = "email.smtp"

  type EmailJob = (to: String, subject: String, body: String)

  def layer(conf: ProviderConf): ZLayer[Any, Nothing, EmailProvider] =
    ZLayer.scoped:
      make(conf)
  
  def make(conf: ProviderConf): ZIO[Any, Nothing, QueuedEmailProvider] = ZIO.scoped:
      for // todo: scoped and eventually release resources
        queue <- Queue.unbounded[EmailJob]
        sigterm <- Promise.make[Nothing, Unit]
        _ <- workers(queue)(using conf, sigterm).forkScoped
        _ <- ZIO.addFinalizer:
          ZIO.logInfo("shutdown, draining email queue...") *> sigterm.succeed(())
      yield new QueuedEmailProvider(conf, queue)

  // todo: read parallelism from configuration
  private def workers(queue: Queue[EmailJob], parallelism: Int = 1)(using c: ProviderConf, sigterm: Promise[Nothing, Unit]): UIO[Unit] =
    ZIO.foreachParDiscard(1 to parallelism)(_ => worker(queue))
    
  private def worker(queue: Queue[EmailJob])(using c: ProviderConf, sigterm: Promise[Nothing, Unit]): UIO[Unit] =

    def work(j: EmailJob): UIO[Unit] =
      for
        _ <- ZIO logInfo s"sending email to ${j.to}..."
        _ <- ZIO
          .attempt:
            val e = newEmail
            // todo: add full name for sender
            e.addTo(j.to)
            e.setSubject(j.subject)
            e.setMsg(j.body)
            e.send()

          .tap(_ => ZIO logInfo s"email sent to ${j.to}")
          .tapError(t => ZIO logError s"failed to send email to ${j.to}: ${t.getMessage}")
          .retry(Schedule.exponential(100.millis) && Schedule.recurs(5))
          .catchAll(_ => ZIO.unit)
      yield ()

    val loop = (queue.take flatMap work).forever

    loop.raceFirst:
      for {
        _ <- sigterm.await
        r <- queue.takeAll
        _ <- ZIO.foreachDiscard(r)(work)
        _ <- ZIO.logInfo("email queue drained on shutdown")
      } yield ()

  private def newEmail(using c: ProviderConf): SimpleEmail =
    val email = new SimpleEmail

    email.setHostName(c.setting("host"))
    email.setSmtpPort(c.setting("port"))
    email.setAuthentication(c.setting("user"), c.setting("pass"))

    val ch: String = c.setting("channel")

    email.setStartTLSEnabled(ch == "STARTTLS")
    email.setSSLOnConnect(ch == "SSL")

    email.setFrom(c.setting("from"), c.setting("name"))

    email.setDebug(c.setting("debug"))
    email
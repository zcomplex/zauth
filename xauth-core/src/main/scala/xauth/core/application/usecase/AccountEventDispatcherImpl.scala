package xauth.core.application.usecase

import xauth.core.common.model.ContactType.Email
import xauth.core.domain.code.model.AccountCodeType.Activation
import xauth.core.domain.code.model.UserContactData
import xauth.core.domain.code.port.AccountCodeService
import xauth.core.spi.AccountEvent.UserRegistered
import xauth.core.spi.{AccountEvent, AccountEventDispatcher, MessagingService}
import zio.*

private class AccountEventDispatcherImpl(queue: Queue[AccountEvent]) extends AccountEventDispatcher:
  override def dispatch(event: AccountEvent): UIO[Unit] =
    queue.offer(event).unit

object AccountEventDispatcherImpl:
  val layer: ZLayer[AccountCodeService & MessagingService, Nothing, AccountEventDispatcher] =
    ZLayer.scoped:
      for
        queue <- Queue.unbounded[AccountEvent]
        sigterm <- Promise.make[Nothing, Unit]
        codes <- ZIO.service[AccountCodeService]
        messaging <- ZIO.service[MessagingService]
        _ <- workers(queue)(using codes, messaging, sigterm).forkScoped
        _ <- ZIO.addFinalizer:
          ZIO.logInfo("shutdown, draining account event queue...") *> sigterm.succeed(())
      yield new AccountEventDispatcherImpl(queue)

  // todo: read parallelism from configuration
  private def workers(queue: Queue[AccountEvent], parallelism: Int = 1)
                     (using codes: AccountCodeService, messaging: MessagingService, sigterm: Promise[Nothing, Unit]): UIO[Unit] =
    ZIO.foreachParDiscard(1 to parallelism)(_ => worker(queue))

  private def worker(queue: Queue[AccountEvent])
                    (using codes: AccountCodeService, messaging: MessagingService, sigterm: Promise[Nothing, Unit]): UIO[Unit] =
    def work(e: AccountEvent): UIO[Unit] =
      for
        _ <- ZIO debug s"processing account event..."
        _ <- ZIO
          .attempt:
            e match
              case UserRegistered(u, w) =>
                for
                  // creating new activation code for the first untrusted email
                  c <- codes.create(Activation, u.id, u.info.contacts.find(c => c.kind == Email && !c.trusted).map(UserContactData.apply))(using w)
                  _ <- messaging.sendActivationMessage(u, c)(using w).forkDaemon
                    *> messaging.notifyUserRegistration(u)(using w).forkDaemon
                yield ()
          .tap(_ => ZIO logInfo s"event processed")
          .tapError(t => ZIO logError s"failed to process event")
          .retry(Schedule.exponential(100.millis) && Schedule.recurs(5))
          .catchAll(_ => ZIO.unit)
      yield ()

    val loop = (queue.take flatMap work).forever

    loop.raceFirst:
      for {
        _ <- sigterm.await
        r <- queue.takeAll
        _ <- ZIO.foreachDiscard(r)(work)
        _ <- ZIO.logInfo("account events queue drained on shutdown")
      } yield ()
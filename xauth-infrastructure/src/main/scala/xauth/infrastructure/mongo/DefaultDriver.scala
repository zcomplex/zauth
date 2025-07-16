package xauth.infrastructure.mongo

import reactivemongo.api.MongoConnection.fromString
import reactivemongo.api.{AsyncDriver, DB, MongoConnection}
import xauth.util.mongo.{Driver, MongoError}
import zio.{IO, ZIO, ZLayer}

private class DefaultDriver(driver: AsyncDriver) extends Driver[MongoConnection, DB]:
  override def connect(uri: String): IO[MongoError, MongoConnection] =
    ZIO
      .fromFuture[MongoConnection]:
        implicit ec => fromString(uri) flatMap driver.connect
      .mapError:
        t => MongoError.ConnectionFailed(t.getMessage)

  override def database(name: String)(using c: MongoConnection): IO[MongoError, DB] =
    ZIO
      .fromFuture:
        implicit ec => c.database(name)
      .mapError:
        t => MongoError.DatabaseNotFound(t.getMessage)

  override def close: IO[MongoError, Unit] =
    ZIO
      .fromFuture:
        implicit ec => driver.close()
      .mapError:
        t => MongoError.AccessFailed(t.getMessage)

object DefaultDriver:
  def layer: ZLayer[Any, Nothing, Driver[MongoConnection, DB]] =
    ZLayer.fromZIO:
      ZIO
        .attempt(new DefaultDriver(new AsyncDriver))
        .tapError(e => ZIO.logError(s"error during default driver creation: ${e.getMessage}"))
        .orDie
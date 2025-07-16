package xauth.util.mongo

import xauth.util.mongo.MongoClient.{UriRegex, resolveKeys}
import xauth.util.mongo.MongoError.AccessFailed
import zio.*
import zio.stm.TMap

import java.nio.file.Paths

/** Defines the basic error type for all client errors. */
sealed trait MongoError extends Throwable

object MongoError:
  /** No handled database for the given identifier. */
  case class NoSuchDatabase[I](id: I)         extends MongoError
  /** The given identifier already corresponds to a handled database. */
  case class AlreadyHandled[I](id: I)         extends MongoError
  /** The given URI cannot be parsed. */
  case class InvalidUri(uri: String)          extends MongoError
  /** Error during the database access. */
  case class AccessFailed(reason: String)     extends MongoError
  /** Remote database not found. */
  case class DatabaseNotFound(reason: String) extends MongoError
  /** Error during the connection process. */
  case class ConnectionFailed(reason: String) extends MongoError

/**
 * Defines the driver trait to supply connection,
 * access and release with the persistence system.
 * @tparam C Connection type that underlying library will return.
 * @tparam D Database type of the underlying library that provides access to the collections
 *           on the persistence system.
 */
trait Driver[C, D]:
  /** Establishes a connection with the server and returns the connection pool object. */
  def connect(uri: String): IO[MongoError, C]
  /** Releases all connections established by the driver. */
  def close: IO[MongoError, Unit]
  /** Returns a reference to the database that gives access on their collections. */
  def database(name: String)(using c: C): IO[MongoError, D]

/**
 * Supplies information about a connection to establish or about a handled database on which operate.
 * @tparam I The identifier type used to store handled databases.
 */
trait Workspace[I]:
  lazy val workspaceId: I
  lazy val connectionUri: String

/** Represents the collection on persistence system. */
abstract class Collection(val name: String)

/** The system collection on the root database that keep track of other tenants. */
abstract class SystemCollection(name: String) extends Collection(name)

/** The workspace collection on the other databases. */
abstract class WorkspaceCollection(name: String) extends Collection(name)

/**
 * Enables to access the root database collections.
 * @tparam C System collection type.
 * @tparam I The identifier type used by the client to store handled databases.
 * @tparam G Usually is generic collection type of the underlying library that supply read/write methods.
 */
trait SystemColl[C <: Collection, I, G]:
  /** Returns the specified collection for the given collection type. */
  def collection(t: C): IO[MongoError, G]
  /** Returns a sequence of collections for the given collection type and for each handled workspace. */
  def collections(t: C): IO[MongoError, Seq[(I, G)]]

/**
 * Enables to access the database collections for a given workspace.
 * @tparam C Workspace collection type.
 * @tparam I The identifier type used by the client to store handled databases.
 * @tparam W Workspace type that supply information about the handled database identifier and connection information.
 * @tparam G Usually is generic collection type of the underlying library that supply read/write methods.
 */
trait WorkspaceColl[C <: Collection, I, W <: Workspace[I], G]:
  /** Returns the specified collection for the given workspace. */
  def collection(t: C)(using w: W): IO[MongoError, G]
  /** Returns a sequence of collections for the given collection type and workspaces. */
  def collections(t: C)(using ws: Seq[W]): IO[MongoError, Seq[(W, G)]]

/**
 * Represents a multi-tenant database client for MongoDB,
 * this abstraction allows to customize access to the persistence system
 * @param databases Stores all database references.
 * @param driver Driver that interacts with the real third-part driver that supply connections and database references.
 * @tparam C Connection type of the third-part driver library.
 * @tparam I The identifier type used by the client to store handled databases.
 * @tparam D Database type of the underlying library that provides access to the collections on the persistence system.
 * @tparam G Usually is generic collection type of the underlying library that supply read/write methods.
 */
abstract class MongoClient[C, I, D, G](databases: TMap[I, D], driver: Driver[C, D]):

  /** Retrieves the handled database reference by its identifier. */
  def database(id: I): IO[MongoError, D] =
    databases
      .get(id)
      .commit
      .filterOrElse(_.nonEmpty)(ZIO.fail(MongoError.NoSuchDatabase(id)))
      .map(_.get)

  def connect(w: Workspace[I]): IO[MongoError, D] =
    connect(w.workspaceId -> w.connectionUri)

  /** Performs new connection, keeps track of it and returns the database reference. */
  def connect(w: (workspaceId: I, connectionUri: String)): IO[MongoError, D] = {
    val resolvedUri = resolveKeys(w.connectionUri)

    for
      // failure if already exists a connection for the given id
      exists <- databases.contains(w.workspaceId).commit
      _ <- ZIO
        .fail(MongoError.AlreadyHandled(w.workspaceId))
        .when(exists)
      
      // retrieving database name from connection string
      dbName <- ZIO
        .fromOption(UriRegex.findFirstMatchIn(resolvedUri).map(_.group("db")))
        .mapError(_ => MongoError.InvalidUri(resolvedUri))

      _ <- ZIO.logInfo(s"connecting system database to '$resolvedUri'")

      // connecting for current driver
      connection <- driver
        .connect(resolvedUri)
        .retry(Schedule.exponential(100.millis) && Schedule.recurs(5))

      // connecting to the database
      db <- driver
        .database(dbName)(using connection)
        .mapError(t => MongoError.AccessFailed(t.getMessage))

      // adding new handled connection
      _ <- databases.put(w.workspaceId, db).commit

    yield db
  }

  /** Releases all connections. */
  def close(): IO[MongoError, Unit] =
    for
      // closing all handled connections
      _    <- driver.close
      // cleaning entries from the pool
      keys <- databases.keys.commit
      _    <- databases.deleteAll(keys).commit
      _    <- ZIO.logInfo(s"released ${keys.size} connections")
    yield ()

  /** Releases handled connection for the given workspace. */
  def close(using w: Workspace[I]): IO[MongoError, Unit]

  // todo: expose log/metrics like alive connections

object MongoClient:
  /** Generic connection URI string for MongoDB. */
  private val UriRegex = "^.*/(?<db>[\\w_-]+)(?:\\?.*)?$".r // todo: use parser and improve it

  /** Resolves keys and returns the URI with full paths for keystores. */
  private def resolveKeys(u: String): String =
    val k = "keyStore"
    // when keyStore key is present, then convert it in absolute format
    s"(?<=$k=)(?<ks>.*)(?>&)".r
      .findFirstMatchIn(u)
      .map(_.group("ks"))
      .map(v => v -> s"file://${Paths.get(".").toFile.getAbsolutePath.replaceAll("/\\.$", "")}/conf/$v")
      .map(t => u.replace(s"$k=${t._1}", s"$k=${t._2}"))
      .getOrElse(u)
    

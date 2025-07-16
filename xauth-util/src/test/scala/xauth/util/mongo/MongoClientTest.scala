package xauth.util.mongo

import zio.stm.TMap
import zio.test.*
import zio.test.Assertion.*
import zio.{IO, ZIO}

object MongoClientTest extends ZIOSpecDefault:

  case class MyConnection()
  case class MyDatabase()

  private val driver: Driver[MyConnection, MyDatabase] = new Driver[MyConnection, MyDatabase]:
    override def connect(uri: String): IO[MongoError, MyConnection] = ZIO.succeed(MyConnection())
    override def close: IO[MongoError, Unit] = ZIO.unit
    override def database(name: String)(using c: MyConnection): IO[MongoError, MyDatabase] = ZIO.succeed(MyDatabase())

  private def newClient(databases: TMap[Int, MyDatabase]) =
    new MongoClient[MyConnection, Int, MyDatabase, String](databases, driver):
      override def close(using w: Workspace[Int]): IO[MongoError, Unit] = ZIO.unit

  private val workspaceZero = new Workspace[Int]:
    override lazy val workspaceId: Int = 0
    override lazy val connectionUri: String = "mongodb://xauth-db:27017/xauth"

  override def spec: Spec[Any, Nothing] = suite("MongoClientTest")(

    test("client.connect should connect to the database"):
      val effect = for
        databases <- TMap.empty[Int, MyDatabase].commit
        client    <- ZIO.succeed(newClient(databases))
        database  <- client.connect(workspaceZero)
        dbCount   <- databases.size.commit
      yield (database, dbCount)

      assertZIO(effect.map(_._1).orDie)(equalTo(MyDatabase())) *>
        assertZIO(effect.map(_._2).orDie)(equalTo(1)),

    test("client.connect should fail for already existing id"):
      val effect = for
        databases <- TMap.empty[Int, MyDatabase].commit
        client    <- ZIO.succeed(newClient(databases))
        database  <- client.connect(workspaceZero)
        _ <- client.connect(workspaceZero)
      yield ()

      assertZIO(effect.exit)(failsWithA[MongoError.AlreadyHandled[?]]),

    test("client.connect should fail for invalid uri"):
      val workspaceWithInvalidUri = new Workspace[Int]:
        override lazy val workspaceId: Int = 0
        override lazy val connectionUri: String = "cluster-zero"

      val effect = for
        databases <- TMap.empty[Int, MyDatabase].commit
        client    <- ZIO.succeed(newClient(databases))
        database  <- client.connect(workspaceWithInvalidUri)
      yield database

      assertZIO(effect.exit)(failsWithA[MongoError.InvalidUri])

  )
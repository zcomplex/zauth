import java.text.SimpleDateFormat

moduleName := "xauth"
organization := "xauth"
organizationName := "X-Auth"

val scala3Version = "3.7.0"

val major = 3
val minor = 0
val patch = 0

def ver(patch: Int = patch) = s"$major.$minor.$patch"

val ZioSchema     = "dev.zio" %% "zio-schema"      % "1.7.3"
val ZioSchemaJson = "dev.zio" %% "zio-schema-json" % "1.7.3"
val ZioLogging    = "dev.zio" %% "zio-logging"     % "2.5.1"

lazy val settings = Seq(
  scalaVersion := scala3Version,
  libraryDependencies ++= Seq(
    ZioSchema,
    "dev.zio" %% "zio" % "2.1.19",
    "dev.zio" %% "zio-test" % "2.1.19" % Test,
    "dev.zio" %% "zio-test-sbt" % "2.1.19" % Test,
    "org.scalameta" %% "munit" % "1.1.1" % Test
  )
)

testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")

def buildInfoTask(dir: String) =
  Compile / sourceGenerators += Def.task {
    import java.util.Date
    val formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss'Z'")
    val file = (Compile / sourceManaged).value / s"/$dir/generated/BuildInfo.scala"
    val contents = IO.read(new File("build-info.scala.template"))
      .replace("{{NAME}}", name.value)
      .replace("{{VERSION}}", version.value)
      .replace("{{BUILT_AT}}", formatter.format(new Date))
    IO.write(file, contents)
    Seq(file)
  }.taskValue

// Project modules definitions

def module(id: String) = s"xauth-$id"

val Core: String = module("core")
val Api: String = module("api")
val ApiModel: String = module("api-model")
val Util: String = module("util")
val Infrastructure: String = module("infrastructure")

// xauth-util
// Common library
lazy val xauthUtil = project
  .in(file(Util))
  .settings(buildInfoTask(Util))
  .settings(settings)
  .settings(
    name := Util,
    version := ver(0)
  )

// xauth-core
// Defines core services, ports and behaviours
lazy val xauthCore = project
  .in(file(Core))
  .dependsOn(xauthUtil)
  .settings(buildInfoTask(Core))
  .settings(settings)
  .settings(
    name := Core,
    version := ver(0),
    libraryDependencies ++= Seq(
      // Scrypt implementation for password encryption
      "com.lambdaworks" % "scrypt" % "1.4.0"
    )
  )

// xauth-api-model
// Defines models for data transport objects, useful to interact with xauth-api rest services.
lazy val xauthApiModel = project
  .in(file(ApiModel))
  .settings(buildInfoTask(ApiModel))
  .settings(settings)
  .settings(
    name := ApiModel,
    version := ver(0),
    libraryDependencies ++= Seq(/*ZioSchema, */ZioSchemaJson)
  )

// xauth-infrastructure
// Implements various core ports and offers alternatives to interact with third part services like persistence systems.
lazy val xauthInfrastructure = project
  .in(file(Infrastructure))
  .dependsOn(xauthUtil)
  .dependsOn(xauthCore)
  .settings(buildInfoTask(Infrastructure))
  .settings(settings)
  .settings(
    name := Infrastructure,
    version := ver(0),
    libraryDependencies ++= Seq(
      "org.reactivemongo" %% "reactivemongo" % "1.1.0-RC12", // do not use noshaded
      ZioLogging/*ZioSchema, ZioSchemaJson*/,
      "org.slf4j" % "slf4j-api" % "2.0.17"
    )
  )

// xauth-api
// Exposes authentication services via http.
lazy val xauthApi = project
  .in(file(Api))
  .dependsOn(xauthInfrastructure, xauthApiModel)
  .settings(buildInfoTask(Api))
  .settings(settings)
  .settings(
    name := Api,
    version := ver(0),
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-http" % "3.3.3",
      // zio-config
      "dev.zio" %% "zio-config"          % "4.0.4",
      "dev.zio" %% "zio-config-typesafe" % "4.0.4",
      "dev.zio" %% "zio-config-magnolia" % "4.0.4",
      // -
      ZioSchema,
      ZioSchemaJson
    )
  )

// xauth-cli
// Supplies a command line tool that represents the client for xauth-api
lazy val xauthCli = project
  .in(file("xauth-cli"))
  .settings(settings)
  .settings(
    name := "xauth-cli",
    version := ver(0)
  )

lazy val root = project
  .in(file("."))
  .aggregate(xauthUtil, xauthCore, xauthApiModel, xauthInfrastructure, xauthApi, xauthCli)
  .settings(settings)
  .settings(
    name := "xauth",
    version := ver(0)
  )
import java.text.SimpleDateFormat

moduleName := "xauth"
organization := "xauth"
organizationName := "X-Auth"

val scala3Version = "3.7.0"

val major = 3
val minor = 0
val patch = 0

def ver(patch: Int = patch) = s"$major.$minor.$patch"

val ZioSchema     = "dev.zio" %% "zio-schema"      % "1.7.5"
val ZioSchemaJson = "dev.zio" %% "zio-schema-json" % "1.7.5"
val ZioLogging    = "dev.zio" %% "zio-logging"     % "2.5.2"

val CirceCore         = "io.circe" %% "circe-core"           % "0.14.15"
val CirceParser       = "io.circe" %% "circe-parser"         % "0.14.15"
val CirceConfig       = "io.circe" %% "circe-config"         % "0.10.2"
val CirceGeneric      = "io.circe" %% "circe-generic"        % "0.14.15"
val CirceGenericExtra = "io.circe" %% "circe-generic-extras" % "0.14.5-RC1"

lazy val scalaSettings = Seq(
  scalaVersion := scala3Version,
  scalacOptions += "-Xmax-inlines:128000",
)

lazy val settings = Seq(
  libraryDependencies ++= Seq(
    "dev.zio" %% "zio"          % "2.1.23",
    "dev.zio" %% "zio-test"     % "2.1.23" % Test,
    "dev.zio" %% "zio-test-sbt" % "2.1.23" % Test,
    ZioSchema,
    "org.scalameta" %% "munit" % "1.2.1" % Test
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
val ApiModelCirce: String = module("api-model-circe")
val ApiModelZioJson: String = module("api-model-ziojson")
val Util: String = module("util")
val Infrastructure: String = module("infrastructure")

// xauth-util
// Common library
lazy val xauthUtil = project
  .in(file(Util))
  .settings(buildInfoTask(Util))
  .settings(scalaSettings)
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
  .settings(scalaSettings)
  .settings(settings)
  .settings(
    name := Core,
    version := ver(0),
    libraryDependencies ++= Seq(
      // Scrypt implementation for password encryption
      "com.lambdaworks" % "scrypt" % "1.4.0",
      "com.lihaoyi" %% "os-lib" % "0.11.5",
      CirceCore, CirceParser, CirceConfig, CirceGeneric, CirceGenericExtra
    )
  )

// xauth-api-model
// Defines models for data transport objects, useful to interact with xauth-api rest services.
lazy val xauthApiModel = project
  .in(file(ApiModel))
  .settings(buildInfoTask(ApiModel))
  .settings(scalaSettings)
  .settings(settings)
  .settings(
    name := ApiModel,
    version := ver(0)/*,
    libraryDependencies ++= Seq(CirceCore, CirceGeneric)*/
  )

// xauth-api-model-circe
// Defines givens for data transport objects.
//lazy val xauthApiModelCirce = project
//  .in(file(ApiModelCirce))
//  .dependsOn(xauthApiModel)
//  .settings(
//    name := ApiModelCirce,
//    version := ver(0),
//    libraryDependencies ++= Seq(CirceGeneric)
//  )

// xauth-api-model-zio-json
// Defines givens for data transport objects.
lazy val xauthApiModelZioJson = project
  .in(file(ApiModelZioJson))
  .dependsOn(xauthApiModel)
  .settings(scalaSettings)
  .settings(
    name := ApiModelZioJson,
    version := ver(0),
    libraryDependencies ++= Seq(ZioSchemaJson)
  )

// xauth-infrastructure
// Implements various core ports and offers alternatives to interact with third part services like persistence systems.
lazy val xauthInfrastructure = project
  .in(file(Infrastructure))
  .dependsOn(xauthUtil)
  .dependsOn(xauthCore)
  .settings(buildInfoTask(Infrastructure))
  .settings(scalaSettings)
  .settings(settings)
  .settings(
    name := Infrastructure,
    version := ver(0),
    libraryDependencies ++= Seq(
      "org.apache.commons" % "commons-email" % "1.6.0",
      "org.reactivemongo" %% "reactivemongo" % "1.1.0-RC12", // do not use noshaded
      ZioLogging/*ZioSchema, ZioSchemaJson*/,
      "org.slf4j" % "slf4j-api" % "2.0.17"
    )
  )

// xauth-api
// Exposes authentication services via http.
lazy val xauthApi = project
  .in(file(Api))
  .dependsOn(xauthInfrastructure, xauthApiModel, xauthApiModelZioJson)
  .settings(buildInfoTask(Api))
  .settings(scalaSettings)
  .settings(settings)
  .settings(
    name := Api,
    version := ver(0),
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-http" % "3.7.1",
      // JWT
      "com.github.jwt-scala" %% "jwt-circe" % "11.0.3",
      // JWK
      "com.nimbusds" % "nimbus-jose-jwt" % "10.5",
      ZioSchema, ZioSchemaJson,
      CirceCore, CirceGeneric, CirceConfig
    )
  )

// xauth-cli
// Supplies a command line tool that represents the client for xauth-api
lazy val xauthCli = project
  .in(file("xauth-cli"))
  .settings(scalaSettings)
  .settings(settings)
  .settings(
    name := "xauth-cli",
    version := ver(0)
  )

lazy val root = project
  .in(file("."))
  .aggregate(xauthUtil, xauthCore, xauthApiModel, xauthInfrastructure, xauthApi, xauthCli)
  .settings(scalaSettings)
  .settings(settings)
  .settings(
    name := "xauth",
    version := ver(0)
  )
import java.text.SimpleDateFormat

moduleName := "xauth"
organization := "xauth"
organizationName := "X-Auth"

val scala3Version = "3.7.0"

val major = 3
val minor = 0
val patch = 0

def ver(patch: Int = patch) = s"$major.$minor.$patch"

val ZioSchema     = "dev.zio" %% "zio-schema"      % "1.7.0"
val ZioSchemaJson = "dev.zio" %% "zio-schema-json" % "1.7.0"

lazy val settings = Seq(
  scalaVersion := scala3Version,
  libraryDependencies ++= Seq(
    ZioSchema,
    "dev.zio" %% "zio" % "2.1.17",
    "org.scalameta" %% "munit" % "1.1.1" % Test
  )
)

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
val ApiModel: String = module("api-model")

// xauth-core-model
// xauth-core

// xauth-api-model
// xauth-api

lazy val xauthCore = project
  .in(file(Core))
  .settings(buildInfoTask(Core))
  .settings(settings)
  .settings(
    name := Core,
    version := ver(0)
  )

lazy val xauthApiModel = project
  .in(file(ApiModel))
  .settings(buildInfoTask(ApiModel))
  .settings(settings)
  .settings(
    name := ApiModel,
    version := ver(0),
    libraryDependencies ++= Seq(/*ZioSchema, */ZioSchemaJson)
  )

lazy val xauthApi = project
  .in(file("xauth-api"))
  .dependsOn(xauthApiModel)
  .settings(buildInfoTask("xauth-api"))
  .settings(settings)
  .settings(
    name := "xauth-api",
    version := ver(0),
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-http" % "3.2.0",
      ZioSchema,
      ZioSchemaJson
    )
  )

lazy val xauthCli = project
  .in(file("xauth-cli"))
  .settings(settings)
  .settings(
    name := "xauth-cli",
    version := ver(0)
  )

lazy val root = project
  .in(file("."))
  .aggregate(xauthCore, xauthApiModel, xauthApi, xauthCli)
  .settings(settings)
  .settings(
    name := "xauth",
    version := ver(0)
  )


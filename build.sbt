import java.text.SimpleDateFormat

name := "xauth"
organization := "xauth"
organizationName := "X-Auth"

val scala3Version = "3.7.0"

val major = 3
val minor = 0
val patch = 0

def ver(patch: Int = patch) = s"$major.$minor.$patch"

lazy val settings = Seq(
  scalaVersion := scala3Version,
  libraryDependencies += "org.scalameta" %% "munit" % "1.1.1" % Test
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

lazy val xauthApi = project
  .in(file("xauth-api"))
  .settings(buildInfoTask("xauth-api"))
  .settings(settings)
  .settings(
    name := "xauth-api",
    version := ver(0),
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "2.1.17",
      "dev.zio" %% "zio-http" % "3.2.0",
      "dev.zio" %% "zio-schema" % "1.7.0",
      "dev.zio" %% "zio-schema-json" % "1.7.0"
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
  .aggregate(xauthApi, xauthCli)
  .settings(settings)
  .settings(
    name := "xauth",
    version := ver(0)
  )


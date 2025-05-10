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
  libraryDependencies += "org.scalameta" %% "munit" % "1.0.0" % Test
)

lazy val xauthApi = project
  .in(file("xauth-api"))
  .settings(settings)
  .settings(
    name := "xauth-api",
    version := ver(0)
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

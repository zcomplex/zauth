package xauth.api

import zio.*
import zio.http.*
import zio.http.Method.GET
import zio.http.codec.*
import zio.http.endpoint.*
import xauth.generated.BuildInfo
import zio.json.*
import zio.schema.*
import zio.schema.Schema.*

object Main extends ZIOAppDefault:

  case class Info(name: String, version: String, builtAt: String)

  private object Info:
    given schema: Schema[Info] = DeriveSchema.gen[Info]
    given encoder: JsonEncoder[Info] = DeriveJsonEncoder.gen[Info]

  private val infoEndpoint = Endpoint(GET / "info").out[Info]

  private val routes: Routes[Any, Nothing] = Routes(
    GET / Root -> handler(Response.text(s"X-Auth ${BuildInfo.Version}")),
    infoEndpoint.implement(_ => ZIO.succeed(Info(BuildInfo.Name, BuildInfo.Version, BuildInfo.BuiltAt)))
  )

  def run: ZIO[Any, Throwable, Nothing] = Server.serve(routes).provide(Server.default)
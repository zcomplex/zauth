package xauth.api.info

import xauth.api.model.info.Info
import xauth.generated.BuildInfo.*
import zio.ZIO
import zio.http.Method.GET
import zio.http.endpoint.Endpoint
import zio.http.{Route, Routes}

object InfoController:

  import xauth.api.model.ziojson.info.schema

  val GetInfo: Route[Any, Nothing] = Endpoint(GET / "info")
    .out[Info]
    .implement:
      _ => ZIO.succeed:
        Info(Name, Version, BuiltAt)

  val routes: Routes[Any, Nothing] = Routes(GetInfo)
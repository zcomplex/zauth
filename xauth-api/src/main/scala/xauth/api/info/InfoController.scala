package xauth.api.info

import xauth.api.model.info.Info
import xauth.generated.BuildInfo.*
import zio.ZIO
import zio.http.Method.GET
import zio.http.Route
import zio.http.endpoint.Endpoint

object InfoController:

  val GetInfo: Route[Any, Nothing] = Endpoint(GET / "info")
    .out[Info]
    .implement:
        _ => ZIO.succeed:
          Info(Name, Version, BuiltAt)
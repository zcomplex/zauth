package xauth.api.tenant

import zio.ZIO
import zio.http.Method.GET
import zio.http.{Route, Routes}
import zio.http.endpoint.Endpoint

object TenantController:
  
  val PostTenant: Route[Any, Nothing] = Endpoint(GET / "system" / "tenants")
    .out[Unit]
    .implement:
      _ =>
        ZIO.succeed:
          ()

  val routes: Routes[Any, Nothing] = Routes.empty
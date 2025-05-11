package xauth.api.model.info

import zio.json.*
import zio.schema.*
import zio.schema.Schema.*

case class Info(name: String, version: String, builtAt: String)

object Info:
  given schema: Schema[Info] = DeriveSchema.gen[Info]
  given encoder: JsonEncoder[Info] = DeriveJsonEncoder.gen[Info]
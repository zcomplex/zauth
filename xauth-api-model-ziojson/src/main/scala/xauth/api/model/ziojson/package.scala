package xauth.api.model

import xauth.api.model.auth.{TokenRq, TokenRs}
import xauth.api.model.info.Info
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

package object ziojson:

  import zio.schema.{DeriveSchema, Schema}

  object info:
    given schema: Schema[Info] = DeriveSchema.gen[Info]

  object auth:
    given Schema[TokenRq] = DeriveSchema.gen[TokenRq]
    given JsonDecoder[TokenRq] = DeriveJsonDecoder.gen[TokenRq]
    given JsonEncoder[TokenRq] = DeriveJsonEncoder.gen[TokenRq]

    given Schema[TokenRs] = DeriveSchema.gen[TokenRs]
    given JsonDecoder[TokenRs] = DeriveJsonDecoder.gen[TokenRs]
    given JsonEncoder[TokenRs] = DeriveJsonEncoder.gen[TokenRs]
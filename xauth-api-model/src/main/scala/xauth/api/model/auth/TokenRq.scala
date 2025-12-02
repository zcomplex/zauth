package xauth.api.model.auth

import zio.schema.codec.Decoder

//import io.circe.{Decoder, Encoder}
//import io.circe.generic.semiauto.*

/**
  * Login information supplied for sign-in.
  */
final case class TokenRq(username: String, password: String)

//object TokenReq:
//  given JsonDe[TokenRq] = DeriveJsonDecoder
//  given Encoder[TokenReq] = deriveEncoder
package xauth.api.model.auth

//import io.circe.{Decoder, Encoder}
//import io.circe.generic.semiauto.*

/**
  * Login information supplied for sign-in.
  */
final case class TokenRq(username: String, password: String)

//object TokenReq:
//  given Decoder[TokenReq] = deriveDecoder
//  given Encoder[TokenReq] = deriveEncoder
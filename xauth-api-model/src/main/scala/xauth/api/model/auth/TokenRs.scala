package xauth.api.model.auth

//import io.circe.{Decoder, Encoder}
//import io.circe.generic.semiauto.*
//import zio.http.codec.json.circe.*
/**
  * Authorization information response data.
  */
final case class TokenRs
(
  tokenType: String,
  accessToken: String,
  expiresIn: Int,
  refreshToken: String
)

//object TokenRes:
//  given Decoder[TokenRes] = deriveDecoder
//  given Encoder[TokenRes] = deriveEncoder
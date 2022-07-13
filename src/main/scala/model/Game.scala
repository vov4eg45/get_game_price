package model

import cats.effect.IO
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf

case class Game(title: String, platforms: List[String], price: Double, source: String)

object Game {
  implicit val jsonDecoder: Decoder[Game] = deriveDecoder
  implicit val jsonEncoder: Encoder[Game] = deriveEncoder
  implicit val encoder: EntityEncoder[IO, Seq[Game]] = jsonEncoderOf
}
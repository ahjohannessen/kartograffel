package kartograffel.server

import cats.effect.IO
import eu.timepit.refined.pureconfig._
import eu.timepit.refined.types.net.PortNumber
import eu.timepit.refined.types.string.NonEmptyString
import kartograffel.server.Config.{Db, Http}

final case class Config(
    http: Http,
    db: Db
)

object Config {
  final case class Http(
      host: NonEmptyString,
      port: PortNumber
  )

  final case class Db(
      driver: NonEmptyString,
      url: NonEmptyString,
      user: String,
      password: String
  )

  def load: IO[Config] =
    IO(pureconfig.loadConfigOrThrow[Config])
}

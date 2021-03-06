package kartograffel.client.repository

import cats.data.{Validated, ValidatedNel}
import cats.syntax.all._
import io.circe.parser._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.parser.decode
import kartograffel.shared.domain.model.{Latitude, Longitude, Tag}
import kartograffel.shared.model.{Entity, Graffel, Position}
import org.scalajs.dom
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.{window, PositionError, PositionOptions}

import scala.concurrent.{Future, Promise}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scalatags.JsDom.all._

trait ClientRepository[F[_]] { self =>

  def findTags(pos: Position): F[List[Tag]]

  def findCurrentPosition(): F[Position]

  def saveTag(tag: Tag): F[List[Tag]]

  def findOrCreateGraffel(graffel: Graffel): F[Entity[Graffel]]
}

object ClientRepository {
  val future = new ClientRepository[Future] {

    override def findTags(pos: Position): Future[List[Tag]] = {
      val url = s"/api/graffel/tag?lat=${pos.latitude}&lon=${pos.longitude}"
      Ajax
        .get(url)
        .map(req => decode[List[Tag]](req.responseText))
        .map(_.toTry.get)
    }

    private def convertPosition(in: dom.Position): ValidatedNel[String, Position] = {
      val lat = in.coords.latitude
      val lon = in.coords.longitude
      val refLat: ValidatedNel[String, Latitude] = Validated
        .fromEither(Latitude.from(lat))
        .toValidatedNel
      val refLon: ValidatedNel[String, Longitude] = Validated
        .fromEither(Longitude.from(lon))
        .toValidatedNel
      (refLat, refLon).mapN(Position(_, _))
    }

    private def validationToException(validated: ValidatedNel[String, Position]): Position =
      validated.toEither.left
        .map(msgNel => new RuntimeException(msgNel.foldLeft("")((a, b) => a + "\n" + b)))
        .toTry
        .get

    private def getCurrentPosition: Future[dom.Position] = {
      val opts = js.Object().asInstanceOf[PositionOptions]
      opts.timeout = 30000
      opts.enableHighAccuracy = true
      val promise: Promise[dom.Position] = Promise()
      window.navigator.geolocation.getCurrentPosition(
        { pos: dom.Position =>
          window.console.info(s"found position ${pos.coords.latitude}, ${pos.coords.longitude}")
          promise.success(pos)
        }, { err: PositionError =>
          window.console.warn(s"position error: code = ${err.code}, msg = ${err.message}")
          promise.failure(kartograffel.shared.model.PositionException(err.message))
        }
      )
      promise.future
    }

    override def findCurrentPosition(): Future[Position] =
      getCurrentPosition
        .map(convertPosition)
        .map(validationToException)

    override def saveTag(tag: Tag): Future[List[Tag]] = {
      val url = "/api/graffel/tag"
      val payload = tag.asJson.spaces2
      Ajax
        .post(
          url = url,
          data = payload
        )
        .map(req => decode[List[Tag]](req.responseText))
        .map(_.toTry.get)
    }

    override def findOrCreateGraffel(graffel: Graffel): Future[Entity[Graffel]] = {
      val url = "/api/graffel"
      val payload = graffel.asJson.spaces2
      Ajax
        .put(
          url = url,
          data = payload
        )
        .map(req => decode[Entity[Graffel]](req.responseText))
        .map(_.toTry.get)
    }
  }
}

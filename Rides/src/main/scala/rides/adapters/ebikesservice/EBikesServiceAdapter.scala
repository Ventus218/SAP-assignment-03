package rides.adapters.ebikesservice

import scala.concurrent.Future
import scala.concurrent.ExecutionContextExecutor
import akka.http.scaladsl.Http
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.*
import akka.http.scaladsl.model.StatusCodes.*
import rides.domain.model.*
import rides.domain.errors.*
import rides.ports.EBikesService
import rides.ports.EBikesService.*
import rides.adapters.Marshalling.{*, given}

class EBikesServiceAdapter(private val address: String)(using
    actorSystem: ActorSystem[Any]
) extends EBikesService:

  given ExecutionContextExecutor = actorSystem.executionContext

  private val ebikesEndpoint = s"http://$address/ebikes"

  override def find(id: EBikeId): Future[Option[EBike]] =
    for
      res <- Http().singleRequest(
        HttpRequest(uri = s"$ebikesEndpoint/${id.value}")
      )
      bike <- res.status match
        case StatusCodes.NotFound => Future(None)
        case _                    => Unmarshal(res).to[EBike].map(Some(_))
    yield bike

  override def eBikes(): Future[Iterable[EBikeId]] =
    for
      res <- Http().singleRequest(
        HttpRequest(uri = ebikesEndpoint)
      )
      bikes <- Unmarshal(res).to[Array[EBike]]
    yield bikes.map(_.id)

  override def updatePhisicalData(
      eBikeId: EBikeId,
      dto: UpdateEBikePhisicalDataDTO
  ): Future[Option[EBike]] =
    for
      body <- Marshal(dto).to[MessageEntity]
      res <- Http().singleRequest(
        HttpRequest(
          method = HttpMethods.PATCH,
          uri = s"$ebikesEndpoint/${eBikeId.value}",
          entity = body
        )
      )
      eBike <- res.status match
        case NotFound => Future(Option.empty[EBike])
        case _        => Unmarshal(res).to[EBike].map(Some(_))
    yield (eBike)

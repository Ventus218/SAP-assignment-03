package ebikes.domain;

import scala.concurrent.*
import ebikes.domain.model.*;
import ebikes.ports.persistence.EBikesRepository;
import ebikes.domain.errors.*
import ebikes.ports.persistence.EBikesEventStore.*

class EBikesServiceImpl(private val eBikesEventStore: EBikesEventStore)
    extends EBikesService:

  override def register(
      id: EBikeId,
      location: V2D,
      direction: V2D
  )(using ec: ExecutionContext): Future[Either[EBikeIdAlreadyInUse, EBike]] =
    val eBike = EBike(id, location, direction, 0)
    // TODO: check existence
    eBikesEventStore
      .publish(EBikeEvent.Registered(EBike(id, location, direction, 0)))
      .map(_ => Right(eBike))

  override def find(id: EBikeId)(using
      ec: ExecutionContext
  ): Future[Option[EBike]] =
    // eBikesRepository.find(id)
    ???

  override def eBikes()(using ec: ExecutionContext): Future[Iterable[EBike]] =
    // eBikesRepository.getAll()
    ???

  override def updatePhisicalData(
      eBikeId: EBikeId,
      location: Option[V2D],
      direction: Option[V2D],
      speed: Option[Double]
  )(using ec: ExecutionContext): Future[Option[EBike]] =
    // eBikesRepository
    //   .update(
    //     eBikeId,
    //     eBike =>
    //       val newLocation = location.getOrElse(eBike.location)
    //       val newDirection = direction.getOrElse(eBike.direction)
    //       val newSpeed = speed.getOrElse(eBike.speed)
    //       eBike.copy(eBikeId, newLocation, newDirection, newSpeed)
    //   )
    //   .toOption
    ???

  def healthCheckError(): Option[String] = None

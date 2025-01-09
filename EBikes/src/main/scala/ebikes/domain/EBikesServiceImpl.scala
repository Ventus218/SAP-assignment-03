package ebikes.domain;

import ebikes.domain.model.*;
import ebikes.ports.persistence.EBikesRepository;
import ebikes.domain.errors.*

class EBikesServiceImpl(private val eBikesRepository: EBikesRepository)
    extends EBikesService:

  override def register(
      id: EBikeId,
      location: V2D,
      direction: V2D
  ): Either[EBikeIdAlreadyInUse, EBike] =
    val eBike = EBike(id, location, direction, 0)
    eBikesRepository.insert(id, eBike) match
      case Left(value)  => Left(EBikeIdAlreadyInUse(id))
      case Right(value) => Right(eBike)

  override def find(id: EBikeId): Option[EBike] =
    eBikesRepository.find(id)

  override def eBikes(): Iterable[EBike] =
    eBikesRepository.getAll()

  override def updatePhisicalData(
      eBikeId: EBikeId,
      location: Option[V2D],
      direction: Option[V2D],
      speed: Option[Double]
  ): Option[EBike] =
    eBikesRepository
      .update(
        eBikeId,
        eBike =>
          val newLocation = location.getOrElse(eBike.location)
          val newDirection = direction.getOrElse(eBike.direction)
          val newSpeed = speed.getOrElse(eBike.speed)
          eBike.copy(eBikeId, newLocation, newDirection, newSpeed)
      )
      .toOption

  def healthCheckError(): Option[String] = None

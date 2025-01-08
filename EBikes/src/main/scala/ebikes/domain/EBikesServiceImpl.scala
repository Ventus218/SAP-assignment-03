package ebikes.domain;

import scala.concurrent.*
import ebikes.domain.errors.*
import ebikes.domain.model.*;
import ebikes.domain.model.EBikeEvent.*
import ebikes.ports.persistence.EBikesEventStore

class EBikesServiceImpl(private val eBikesEventStore: EBikesEventStore)
    extends EBikesService:

  override def register(
      id: EBikeId,
      location: V2D,
      direction: V2D
  )(using ec: ExecutionContext): Future[Either[EBikeIdAlreadyInUse, EBike]] =
    val eBike = EBike(id, location, direction, 0)
    for
      events <- eBikesEventStore.allEvents()
      alreadyExists = events.exists(_ match
        case Registered(eBike) if eBike.id == id => true
        case _                                   => false
      )
      res <-
        if alreadyExists then Future(Left(EBikeIdAlreadyInUse(id)))
        else
          eBikesEventStore
            .publish(EBikeEvent.Registered(eBike))
            .map(_ => Right(eBike))
    yield (res)

  override def find(id: EBikeId)(using
      ec: ExecutionContext
  ): Future[Option[EBike]] =
    eBikesEventStore.find(id)

  override def eBikes()(using ec: ExecutionContext): Future[Iterable[EBike]] =
    eBikesEventStore.all()

  override def updatePhisicalData(
      eBikeId: EBikeId,
      location: Option[V2D],
      direction: Option[V2D],
      speed: Option[Double]
  )(using ec: ExecutionContext): Future[Either[EBikeNotFound, Unit]] =
    for
      eBike <- find(eBikeId)
      res <-
        if eBike.isDefined then
          eBikesEventStore
            .publish(UpdatedPhisicalData(eBikeId, location, direction, speed))
            .map(Right(_))
        else Future(Left(EBikeNotFound(eBikeId)))
    yield (res)

  def healthCheckError(): Option[String] = None

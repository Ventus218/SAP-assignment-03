package ebikes.domain;

import scala.concurrent.*
import ebikes.domain.model.*;
import ebikes.domain.errors.*

trait EBikesService:

  def find(id: EBikeId)(using ec: ExecutionContext): Future[Option[EBike]]

  def eBikes()(using ec: ExecutionContext): Future[Iterable[EBike]]

  def register(
      id: EBikeId,
      location: V2D,
      direction: V2D
  )(using ec: ExecutionContext): Future[Either[EBikeIdAlreadyInUse, EBike]]

  def updatePhisicalData(
      eBikeId: EBikeId,
      location: Option[V2D],
      direction: Option[V2D],
      speed: Option[Double]
  )(using ec: ExecutionContext): Future[Either[EBikeNotFound, Unit]]

  def healthCheckError(): Option[String]

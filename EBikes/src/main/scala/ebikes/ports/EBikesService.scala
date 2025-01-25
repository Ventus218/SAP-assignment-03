package ebikes.ports;

import scala.concurrent.*
import shared.domain.EventSourcing.*
import shared.ports.cqrs.QuerySide.Errors.*
import ebikes.domain.model.*;
import ebikes.domain.errors.*

trait EBikesService:

  def find(id: EBikeId): Option[EBike]

  def eBikes(): Iterable[EBike]

  def register(
      id: EBikeId,
      location: V2D,
      direction: V2D
  )(using ExecutionContext): Future[CommandId]

  def updatePhisicalData(
      eBikeId: EBikeId,
      location: Option[V2D],
      direction: Option[V2D],
      speed: Option[Double]
  )(using ExecutionContext): Future[CommandId]

  def commandResult(
      id: CommandId
  ): Either[CommandNotFound, Either[EBikeCommandErrors, Option[EBike]]]

  def healthCheckError(): Option[String]

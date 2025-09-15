package ebikes.ports;

import scala.concurrent.*
import shared.domain.EventSourcing.*
import shared.ports.cqrs.QuerySide.Errors.*
import ebikes.domain.model.*;
import ebikes.domain.errors.*

trait EBikesService:

  def find(id: EBikeId): Option[EBike]

  def eBikes(): Iterable[EBike]

  def register(id: EBikeId)(using ExecutionContext): Future[CommandId]

  def updateLocation(
      eBikeId: EBikeId,
      location: EBikeLocation
  )(using ExecutionContext): Future[CommandId]

  def commandResult(
      id: CommandId
  ): Either[CommandNotFound, Either[EBikeCommandErrors, Option[EBike]]]

  def healthCheckError(using ExecutionContext): Future[Option[String]]

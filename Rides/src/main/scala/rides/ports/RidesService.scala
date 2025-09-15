package rides.ports;

import scala.concurrent.*
import shared.domain.EventSourcing.CommandId
import shared.ports.cqrs.QuerySide.Errors
import rides.domain.model.*;

trait RidesService:

  def find(id: RideId): Option[Ride]

  def activeRides(): Iterable[Ride]

  def startRide(
      eBikeId: EBikeId,
      username: Username,
      junctionId: JunctionId
  )(using ExecutionContext): Future[CommandId]

  def eBikeArrivedToUser(id: RideId)(using ExecutionContext): Future[CommandId]

  def userStoppedRiding(id: RideId)(using ExecutionContext): Future[CommandId]

  def eBikeReachedStation(id: RideId)(using ExecutionContext): Future[CommandId]

  def availableEBikes(): Iterable[EBikeId]

  def commandResult(
      id: CommandId
  ): Either[Errors.CommandNotFound, Either[RideCommandError, Option[Ride]]]

  def healthCheckError(using ExecutionContext): Future[Option[String]]

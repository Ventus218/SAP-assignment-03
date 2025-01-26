package rides.ports;

import rides.domain.model.*;
import rides.domain.errors.*

trait RidesService:

  def find(id: RideId): Option[Ride]

  def activeRides(): Iterable[Ride]

  type StartRideError = UserOrEBikeAlreadyOnARide | UserOrEBikeDoesNotExist |
    FailureInOtherService

  def startRide(
      eBikeId: EBikeId,
      username: Username
  ): Either[StartRideError, Ride]

  def endRide(id: RideId): Either[RideNotFound, Ride]

  def availableEBikes(): Iterable[EBikeId]

  def healthCheckError(): Option[String]

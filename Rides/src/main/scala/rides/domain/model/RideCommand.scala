package rides.domain.model

import shared.domain.EventSourcing.*
import rides.ports.*

case class RideCommandEnviroment(
    eBikesService: EBikesService,
    usersService: UsersService
)

sealed trait RideCommand
    extends Command[
      RideId,
      Ride,
      RideCommandError,
      RideCommandEnviroment,
      RideCommand
    ]

sealed trait RideCommandError
object RideCommandError:
  sealed trait StartRideCommandError extends RideCommandError
  case class RideNotFound(id: RideId) extends RideCommandError
  case class RideAlreadyEnded(id: RideId) extends RideCommandError
  case class EBikeNotFound(id: EBikeId)
      extends RideCommandError,
        StartRideCommandError
  case class UserNotFound(id: Username)
      extends RideCommandError,
        StartRideCommandError
  case class EBikeAlreadyInUse(id: EBikeId)
      extends RideCommandError,
        StartRideCommandError
  case class UserAlreadyRiding(id: Username)
      extends RideCommandError,
        StartRideCommandError

object RideCommand:
  import RideCommandError.*

  case class StartRide(
      id: CommandId,
      entityId: RideId,
      eBikeId: EBikeId,
      username: Username,
      timestamp: Option[Long] = None
  ) extends RideCommand:

    def setTimestamp(timestamp: Long): StartRide =
      copy(timestamp = Some(timestamp))

    override def apply(entities: Map[RideId, Ride])(using
        env: RideCommandEnviroment
    ): Either[StartRideCommandError, Map[RideId, Ride]] =
      assert(timestamp != None)
      lazy val bikeIsFree = !entities.values.exists(_.eBikeId == eBikeId)
      lazy val userIsFree = !entities.values.exists(_.username == username)
      for
        _ <- Either.cond(bikeIsFree, (), EBikeAlreadyInUse(eBikeId))
        _ <- Either.cond(userIsFree, (), UserAlreadyRiding(username))
        bikeExist = env.eBikesService.exists(eBikeId, timestamp.get)
        userExist = env.usersService.exists(username, timestamp.get)
        _ <- Either.cond(bikeExist, (), EBikeNotFound(eBikeId))
        _ <- Either.cond(userExist, (), UserNotFound(username))
        ride = Ride(entityId, eBikeId, username, java.util.Date(), None)
      yield (entities + (ride.id -> ride))

  case class EndRide(
      id: CommandId,
      entityId: RideId,
      timestamp: Option[Long] = None
  ) extends RideCommand:

    def setTimestamp(timestamp: Long): EndRide =
      copy(timestamp = Some(timestamp))

    def apply(entities: Map[RideId, Ride])(using
        RideCommandEnviroment
    ): Either[RideNotFound | RideAlreadyEnded, Map[RideId, Ride]] =
      entities.get(entityId) match
        case None => Left(RideNotFound(entityId))
        case Some(ride) if ride.end.isDefined =>
          Left(RideAlreadyEnded(entityId))
        case Some(ride) =>
          Right(entities + (ride.id -> ride.copy(end = Some(java.util.Date()))))

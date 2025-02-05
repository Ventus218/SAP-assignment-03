package rides.domain.model

import java.util.Date
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
  case class BadCommand(reason: String) extends RideCommandError

object RideCommand:
  import RideCommandError.*
  import RideStatus.*

  case class StartRide(
      id: CommandId,
      entityId: RideId,
      eBikeId: EBikeId,
      username: Username,
      junctionId: JunctionId,
      timestamp: Option[Long] = None
  ) extends RideCommand:

    def setTimestamp(timestamp: Long): StartRide =
      copy(timestamp = Some(timestamp))

    override def apply(entities: Map[RideId, Ride])(using
        env: RideCommandEnviroment
    ): Either[StartRideCommandError, Map[RideId, Ride]] =
      assert(timestamp != None)
      lazy val bikeIsFree =
        !entities.values.exists(r =>
          r.eBikeId == eBikeId && !r.status.isInstanceOf[Ended]
        )
      lazy val userIsFree =
        !entities.values.exists(r =>
          r.username == username && !r.status.isInstanceOf[Ended]
        )
      for
        _ <- Either.cond(bikeIsFree, (), EBikeAlreadyInUse(eBikeId))
        _ <- Either.cond(userIsFree, (), UserAlreadyRiding(username))
        bikeExist = env.eBikesService.exists(eBikeId, timestamp.get)
        userExist = env.usersService.exists(username, timestamp.get)
        _ <- Either.cond(bikeExist, (), EBikeNotFound(eBikeId))
        _ <- Either.cond(userExist, (), UserNotFound(username))
        ride = Ride(
          entityId,
          eBikeId,
          username,
          Date(timestamp.get),
          BikeGoingToUser(junctionId)
        )
      yield (entities + (ride.id -> ride))

  case class EBikeArrivedToUser(
      id: CommandId,
      entityId: RideId,
      timestamp: Option[Long] = None
  ) extends RideCommand:

    override def setTimestamp(timestamp: Long): EBikeArrivedToUser =
      copy(timestamp = Some(timestamp))

    override def apply(entities: Map[RideId, Ride])(using
        RideCommandEnviroment
    ): Either[RideNotFound | BadCommand, Map[RideId, Ride]] =
      entities.get(entityId) match
        case None => Left(RideNotFound(entityId))
        case Some(r) if r.status.isInstanceOf[BikeGoingToUser] =>
          Right(entities + (entityId -> r.copy(status = UserRiding)))
        case Some(r) =>
          Left(
            BadCommand(
              s"EBikeArrivedToUser not appliable in status ${r.status}"
            )
          )

  case class UserStoppedRiding(
      id: CommandId,
      entityId: RideId,
      timestamp: Option[Long] = None
  ) extends RideCommand:

    override def setTimestamp(timestamp: Long): UserStoppedRiding =
      copy(timestamp = Some(timestamp))

    override def apply(entities: Map[RideId, Ride])(using
        RideCommandEnviroment
    ): Either[RideNotFound | BadCommand, Map[RideId, Ride]] =
      entities.get(entityId) match
        case None => Left(RideNotFound(entityId))
        case Some(r) if r.status == UserRiding =>
          Right(
            entities + (entityId -> r.copy(status = BikeGoingBackToStation))
          )
        case Some(r) =>
          Left(
            BadCommand(s"UserStoppedRiding not appliable in status ${r.status}")
          )

  case class EBikeReachedStation(
      id: CommandId,
      entityId: RideId,
      timestamp: Option[Long] = None
  ) extends RideCommand:

    def setTimestamp(timestamp: Long): EBikeReachedStation =
      copy(timestamp = Some(timestamp))

    def apply(entities: Map[RideId, Ride])(using
        RideCommandEnviroment
    ): Either[RideNotFound | BadCommand, Map[RideId, Ride]] =
      entities.get(entityId) match
        case None => Left(RideNotFound(entityId))
        case Some(ride) if ride.status == BikeGoingBackToStation =>
          Right(
            entities + (ride.id -> ride.copy(status =
              Ended(Date(timestamp.get))
            ))
          )
        case Some(ride) =>
          Left(
            BadCommand(
              s"BikeReachedStation not appliable in status ${ride.status}"
            )
          )

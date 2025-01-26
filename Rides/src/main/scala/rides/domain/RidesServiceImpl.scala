package rides.domain;

import java.util.*
import scala.concurrent.*
import rides.domain.model.*
import rides.domain.errors.*
import rides.domain.errors.UserOrEBikeAlreadyOnARide.*
import rides.domain.errors.UserOrEBikeDoesNotExist.*
import rides.ports.persistence.RidesRepository
import rides.ports.*
import rides.domain.errors.UserOrEBikeAlreadyOnARide.EBikeAlreadyOnARide

class RidesServiceImpl(
    private val ridesRepository: RidesRepository,
    private val eBikesService: EBikesService,
    private val usersService: UsersService
)(using
    executionContext: ExecutionContext
) extends RidesService:

  private val transactionLock = java.util.concurrent.locks.ReentrantLock(true)

  def transaction[T](f: => T): T =
    try {
      transactionLock.lock()
      f
    } finally {
      transactionLock.unlock()
    }

  def find(id: RideId): Option[Ride] =
    transaction:
      ridesRepository.find(id)

  def activeRides(): Iterable[Ride] =
    transaction:
      ridesRepository.getAll().filter(_.end.isEmpty)

  def startRide(
      eBikeId: EBikeId,
      username: Username
  ): Either[StartRideError, Ride] =
    transaction:
      val activeRides = this.activeRides()

      lazy val bikeIsFree = !activeRides.exists(_.eBikeId == eBikeId)
      lazy val userIsFree = !activeRides.exists(_.username == username)
      val checkEBikeAndUserFree = for
        _ <- Either.cond(bikeIsFree, (), EBikeAlreadyOnARide(eBikeId))
        _ <- Either.cond(userIsFree, (), UserAlreadyOnARide(username))
      yield ()

      checkEBikeAndUserFree match
        case Left(error) => Left(error)
        case Right(_) =>
          val bikeExist = eBikesService.exists(eBikeId)
          val userExist = usersService.exists(username)
          val eBikeAndUserExist =
            for
              _ <- Either.cond(bikeExist, (), EBikeDoesNotExist(eBikeId))
              _ <- Either.cond(userExist, (), UserDoesNotExist(username))
            yield ()

          eBikeAndUserExist match
            case Left(error) => Left(error)
            case Right(_) =>
              val id = RideId(UUID.randomUUID().toString())
              val ride = Ride(id, eBikeId, username, Date(), None)
              ridesRepository.insert(id, ride) match
                case Left(value)  => throw Exception("UUID collision... WTF")
                case Right(value) => Right(ride)

  def endRide(id: RideId): Either[RideNotFound, Ride] =
    transaction:
      ridesRepository.update(
        id,
        r => r.copy(end = r.end.orElse(Some(Date())))
      ) match
        case Left(value) => Left(RideNotFound(id))
        case Right(ride) => Right(ride)

  def availableEBikes(): Iterable[EBikeId] =
    transaction:
      eBikesService.eBikes() -- activeRides().map(_.eBikeId)

  def healthCheckError(): Option[String] =
    transaction:
      None

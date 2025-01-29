package rides.domain;

import java.util.*
import scala.concurrent.*
import shared.domain.EventSourcing.CommandId
import rides.domain.model.*
import rides.ports.persistence.RidesRepository
import rides.ports.*
import rides.ports.cqrs.*
import shared.ports.cqrs.QuerySide.Errors.CommandNotFound

class RidesServiceImpl(
    private val ridesRepository: RidesRepository,
    private val eBikesService: EBikesService,
    private val usersService: UsersService,
    private val commandSide: RidesCommandSide,
    private val querySide: RidesQuerySide
) extends RidesService:

  given RideCommandEnviroment =
    RideCommandEnviroment(eBikesService, usersService)
  def find(id: RideId): Option[Ride] =
    querySide.find(id)

  def activeRides(): Iterable[Ride] =
    querySide.getAll().filter(_.end.isEmpty)

  def startRide(
      eBikeId: EBikeId,
      username: Username
  )(using ExecutionContext): Future[CommandId] =
    val rideId = RideId(UUID.randomUUID().toString())
    val command =
      RideCommand.StartRide(CommandId.random(), rideId, eBikeId, username)
    commandSide.publish(command).map(_ => command.id)

  def endRide(id: RideId)(using ExecutionContext): Future[CommandId] =
    ???
    // ridesRepository.update(
    //   id,
    //   r => r.copy(end = r.end.orElse(Some(Date())))
    // ) match
    //   case Left(value) => Left(RideNotFound(id))
    //   case Right(ride) => Right(ride)

  def availableEBikes(): Iterable[EBikeId] =
    eBikesService.eBikes() -- activeRides().map(_.eBikeId)

  def commandResult(
      id: CommandId
  ): Either[CommandNotFound, Either[RideCommandError, Option[Ride]]] =
    querySide.commandResult(id) match
      case Left(value) => Left(CommandNotFound(value.id))
      case Right(value) =>
        val command = querySide.commands().find(_.id == id).get
        Right(value.map(_.get(command.entityId)))

  def healthCheckError(): Option[String] =
    None

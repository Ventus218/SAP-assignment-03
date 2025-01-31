package rides.domain;

import scala.concurrent.*
import shared.domain.EventSourcing.CommandId
import rides.domain.model.*
import rides.ports.*
import rides.ports.cqrs.*
import shared.ports.cqrs.QuerySide.Errors.CommandNotFound

class RidesServiceImpl(
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
    querySide.getAll().filter(!_.status.isInstanceOf[RideStatus.Ended])

  def startRide(
      eBikeId: EBikeId,
      username: Username
  )(using ExecutionContext): Future[CommandId] =
    val rideId = RideId(java.util.UUID.randomUUID().toString())
    val command =
      RideCommand.StartRide(CommandId.random(), rideId, eBikeId, username)
    commandSide.publish(command).map(_ => command.id)

  def endRide(id: RideId)(using ExecutionContext): Future[CommandId] =
    val command = RideCommand.EndRide(CommandId.random(), id)
    commandSide.publish(command).map(_ => command.id)

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

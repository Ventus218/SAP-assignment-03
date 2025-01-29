package ebikes.domain;

import scala.concurrent.*
import shared.domain.EventSourcing.*
import shared.ports.cqrs.QuerySide.Errors.*
import ebikes.domain.model.*;
import ebikes.domain.errors.*
import ebikes.ports.persistence.EBikesRepository;
import ebikes.ports.EBikesService
import ebikes.ports.cqrs.*

class EBikesServiceImpl(
    private val commandSide: EBikesCommandSide,
    private val querySide: EBikesQuerySide
) extends EBikesService:

  given Unit = ()
  override def register(
      id: EBikeId,
      location: V2D,
      direction: V2D
  )(using ExecutionContext): Future[CommandId] =
    val command =
      EBikeCommands.Register(CommandId.random(), id, location, direction)
    commandSide.publish(command).map(_ => command.id)

  override def find(id: EBikeId): Option[EBike] =
    querySide.find(id)

  override def eBikes(): Iterable[EBike] =
    querySide.getAll()

  override def updatePhisicalData(
      eBikeId: EBikeId,
      location: Option[V2D],
      direction: Option[V2D],
      speed: Option[Double]
  )(using ExecutionContext): Future[CommandId] =
    val command =
      EBikeCommands.UpdatePhisicalData(
        CommandId.random(),
        eBikeId,
        location,
        direction,
        speed
      )
    commandSide.publish(command).map(_ => command.id)

  override def commandResult(
      id: CommandId
  ): Either[CommandNotFound, Either[EBikeCommandErrors, Option[EBike]]] =
    querySide
      .commandResult(id)
      .map(entities =>
        val command = querySide.commands().find(_.id == id).get
        entities.map(_.get(command.entityId))
      )

  def healthCheckError(): Option[String] = None

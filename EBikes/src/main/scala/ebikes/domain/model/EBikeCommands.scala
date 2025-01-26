package ebikes.domain.model

import shared.domain.EventSourcing.Command
import shared.domain.EventSourcing.CommandId

sealed trait EBikeCommands
    extends Command[EBikeId, EBike, EBikeCommandErrors, Nothing]

enum EBikeCommandErrors:
  case EBikeIdAlreadyInUse(id: EBikeId)
  case EBikeNotFound(id: EBikeId)

object EBikeCommands:
  import EBikeCommandErrors.*

  case class Register(
      id: CommandId,
      entityId: EBikeId,
      location: V2D,
      direction: V2D,
      timestamp: Option[Long] = None
  ) extends EBikeCommands:

    override def apply(
        previous: Option[EBike],
        env: Option[Nothing] = None
    ): Either[EBikeIdAlreadyInUse, Option[EBike]] =
      previous match
        case None        => Right(Some(EBike(entityId, location, direction, 0)))
        case Some(value) => Left(EBikeIdAlreadyInUse(entityId))

  case class UpdatePhisicalData(
      id: CommandId,
      entityId: EBikeId,
      location: Option[V2D],
      direction: Option[V2D],
      speed: Option[Double],
      timestamp: Option[Long] = None
  ) extends EBikeCommands:

    override def apply(
        previous: Option[EBike],
        env: Option[Nothing] = None
    ): Either[EBikeNotFound, Option[EBike]] =
      previous match
        case None => Left(EBikeNotFound(entityId))
        case Some(value) =>
          val newLocation = location.getOrElse(value.location)
          val newDirection = direction.getOrElse(value.direction)
          val newSpeed = speed.getOrElse(value.speed)
          Right(
            Some(
              value.copy(
                location = newLocation,
                direction = newDirection,
                speed = newSpeed
              )
            )
          )

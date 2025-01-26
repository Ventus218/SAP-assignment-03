package ebikes.domain.model

import shared.domain.EventSourcing.*

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

    override def apply(entities: Map[EBikeId, EBike])(using
        Option[Environment[Nothing]]
    ): Either[EBikeIdAlreadyInUse, Map[EBikeId, EBike]] =
      entities.get(entityId) match
        case None =>
          Right(
            entities + (entityId -> EBike(entityId, location, direction, 0))
          )
        case Some(value) => Left(EBikeIdAlreadyInUse(entityId))

  case class UpdatePhisicalData(
      id: CommandId,
      entityId: EBikeId,
      location: Option[V2D],
      direction: Option[V2D],
      speed: Option[Double],
      timestamp: Option[Long] = None
  ) extends EBikeCommands:

    override def apply(entities: Map[EBikeId, EBike])(using
        Option[Environment[Nothing]]
    ): Either[EBikeNotFound, Map[EBikeId, EBike]] =
      entities.get(entityId) match
        case None => Left(EBikeNotFound(entityId))
        case Some(value) =>
          val newLocation = location.getOrElse(value.location)
          val newDirection = direction.getOrElse(value.direction)
          val newSpeed = speed.getOrElse(value.speed)
          Right(
            entities + (
              entityId ->
                value.copy(
                  location = newLocation,
                  direction = newDirection,
                  speed = newSpeed
                )
            )
          )

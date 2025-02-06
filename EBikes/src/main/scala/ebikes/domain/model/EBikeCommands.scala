package ebikes.domain.model

import shared.domain.EventSourcing.*

sealed trait EBikeCommands
    extends Command[EBikeId, EBike, EBikeCommandErrors, Unit, EBikeCommands]

enum EBikeCommandErrors:
  case EBikeIdAlreadyInUse(id: EBikeId)
  case EBikeNotFound(id: EBikeId)

object EBikeCommands:
  import EBikeCommandErrors.*

  case class Register(
      id: CommandId,
      entityId: EBikeId,
      location: EBikeLocation,
      timestamp: Option[Long] = None
  ) extends EBikeCommands:

    def setTimestamp(timestamp: Long): Register =
      copy(timestamp = Some(timestamp))

    override def apply(entities: Map[EBikeId, EBike])(using
        Unit
    ): Either[EBikeIdAlreadyInUse, Map[EBikeId, EBike]] =
      entities.get(entityId) match
        case None =>
          Right(entities + (entityId -> EBike(entityId, location)))
        case Some(value) => Left(EBikeIdAlreadyInUse(entityId))

  case class UpdateLocation(
      id: CommandId,
      entityId: EBikeId,
      location: EBikeLocation,
      timestamp: Option[Long] = None
  ) extends EBikeCommands:

    def setTimestamp(timestamp: Long): UpdateLocation =
      copy(timestamp = Some(timestamp))

    override def apply(entities: Map[EBikeId, EBike])(using
        Unit
    ): Either[EBikeNotFound, Map[EBikeId, EBike]] =
      entities.get(entityId) match
        case None => Left(EBikeNotFound(entityId))
        case Some(value) =>
          Right(entities + (entityId -> value.copy(location = location)))

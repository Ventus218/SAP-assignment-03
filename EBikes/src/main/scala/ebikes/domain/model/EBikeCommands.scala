package ebikes.domain.model

import shared.domain.EventSourcing.Command
import shared.domain.EventSourcing.CommandId

sealed trait EBikeCommands extends Command[EBikeId, EBike, EBikeCommandErrors]

enum EBikeCommandErrors:
  case EBikeIdAlreadyInUse(id: EBikeId)

object EBikeCommands:
  import EBikeCommandErrors.*

  case class Register(
      id: CommandId,
      entityId: EBikeId,
      location: V2D,
      direction: V2D
  ) extends EBikeCommands:

    override def apply(
        previous: Option[EBike]
    ): Either[EBikeIdAlreadyInUse, Option[EBike]] =
      previous match
        case None        => Right(Some(EBike(entityId, location, direction, 0)))
        case Some(value) => Left(EBikeIdAlreadyInUse(entityId))

package shared.ports.cqrs

import shared.domain.EventSourcing.*

object QuerySide:
  enum Errors:
    case CommandNotFound(id: CommandId)
  import Errors.*

  trait QuerySide[TId, T <: Entity[TId], Error, C <: Command[TId, T, Error]]:
    def find(id: TId): Option[T]
    def getAll(): Iterable[T]

    def commands(): Iterable[C]

    def commandResult(
        id: CommandId
    ): Either[CommandNotFound, Either[Error, Option[T]]]

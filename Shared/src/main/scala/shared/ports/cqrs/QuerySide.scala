package shared.ports.cqrs

import shared.domain.EventSourcing.*

object QuerySide:
  enum Errors:
    case CommandNotFound(id: CommandId)
  import Errors.*

  trait QuerySide[TId, T <: Entity[TId], Error, Env, C <: Command[
    TId,
    T,
    Error,
    Env,
    C
  ]]:
    def find(id: TId, atTimestamp: Long = Long.MaxValue)(using Env): Option[T]

    def getAll(atTimestamp: Long = Long.MaxValue)(using Env): Iterable[T]

    def commands(atTimestamp: Long = Long.MaxValue): Iterable[C]

    def commandResult(id: CommandId)(using
        Env
    ): Either[CommandNotFound, Either[Error, Map[TId, T]]]

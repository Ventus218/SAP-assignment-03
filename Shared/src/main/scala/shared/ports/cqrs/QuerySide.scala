package shared.ports.cqrs

import shared.domain.EventSourcing.Entity

trait QuerySide[TId, T <: Entity[TId]]:
  def find(id: TId): Option[T]
  def getAll(): Iterable[T]

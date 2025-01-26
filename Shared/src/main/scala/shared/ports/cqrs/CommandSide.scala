package shared.ports.cqrs

import scala.concurrent.*
import shared.domain.EventSourcing.*
import upickle.default.*

trait CommandSide[TId, T <: Entity[TId], Error, Env, C <: Command[
  TId,
  T,
  Error,
  Env
]]:
  def publish(command: C)(using ExecutionContext): Future[Unit]

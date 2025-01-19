package shared.ports

import scala.concurrent.*
import shared.domain.EventSourcing.*
import upickle.default.*

trait CommandSide[TId, T <: Entity[TId], Error, C <: Command[TId, T, Error]]:
  def publish(command: C)(using ExecutionContext): Future[Unit]

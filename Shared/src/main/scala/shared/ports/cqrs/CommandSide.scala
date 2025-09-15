package shared.ports.cqrs

import scala.concurrent.*
import shared.domain.EventSourcing.*
import upickle.default.*

trait CommandSide[TId, T <: Entity[TId], Error, Env, C <: Command[
  TId,
  T,
  Error,
  Env,
  C
]]:
  def publish(command: C)(using ExecutionContext): Future[Unit]

  /** Checks whether the CommandSide is working well
    *
    * @return
    *   Some(errorMessage) if the healtCheck failed
    */
  def healthCheck(using ExecutionContext): Future[Option[String]]

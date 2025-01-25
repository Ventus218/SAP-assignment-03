package ebikes.adapters.cqrs

import upickle.default.*
import shared.domain.EventSourcing.*
import ebikes.domain.model.*

object EBikeCommandsSerialization:
  given ReadWriter[CommandId] = ReadWriter.derived
  given ReadWriter[EBikeId] = ReadWriter.derived
  given ReadWriter[V2D] = ReadWriter.derived
  given ReadWriter[EBikeCommands] = ReadWriter.derived

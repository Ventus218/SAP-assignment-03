package ebikes.adapters.cqrs

import upickle.default.*
import shared.domain.EventSourcing.*
import ebikes.domain.model.*

object EBikeCommandsSerialization:
  given ReadWriter[CommandId] = ReadWriter.derived
  given ReadWriter[EBikeId] = ReadWriter.derived
  given ReadWriter[StreetId] = ReadWriter.derived
  given ReadWriter[JunctionId] = ReadWriter.derived
  given ReadWriter[EBikeLocation] = ReadWriter.derived
  given ReadWriter[EBikeCommands] = ReadWriter.derived

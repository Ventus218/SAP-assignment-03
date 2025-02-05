package rides.adapters.cqrs

import upickle.default.*
import shared.domain.EventSourcing.*
import rides.domain.model.*

object RideCommandsSerialization:
  given ReadWriter[CommandId] = ReadWriter.derived
  given ReadWriter[RideId] = ReadWriter.derived
  given ReadWriter[EBikeId] = ReadWriter.derived
  given ReadWriter[Username] = ReadWriter.derived
  given ReadWriter[JunctionId] = ReadWriter.derived
  given ReadWriter[RideCommand] = ReadWriter.derived

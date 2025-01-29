package rides.adapters.cqrs

import scala.concurrent.*
import upickle.default.*
import shared.adapters.cqrs.CommandSideKafkaAdapter
import rides.domain.model.*
import rides.ports.cqrs.*

import RideCommandsSerialization.given

class RidesCommandSideKafkaAdapter(
    bootstrapServers: String,
    clientId: String,
    topic: String
) extends CommandSideKafkaAdapter[
      RideId,
      Ride,
      RideCommandError,
      RideCommandEnviroment,
      RideCommand
    ](
      bootstrapServers,
      clientId,
      topic
    ),
      RidesCommandSide

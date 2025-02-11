package rides.adapters.cqrs

import shared.adapters.cqrs.QuerySideKafkaAdapter
import shared.domain.EventSourcing.CommandId
import rides.domain.model.*
import rides.ports.cqrs.RidesQuerySide

import RideCommandsSerialization.given

class RidesQuerySideKafkaAdapter(
    bootstrapServers: String,
    topic: String
) extends QuerySideKafkaAdapter[
      RideId,
      Ride,
      RideCommandError,
      RideCommandEnviroment,
      RideCommand
    ](bootstrapServers, topic),
      RidesQuerySide

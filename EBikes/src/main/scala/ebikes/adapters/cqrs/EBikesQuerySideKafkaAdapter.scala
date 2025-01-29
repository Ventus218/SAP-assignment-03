package ebikes.adapters.cqrs

import shared.adapters.cqrs.QuerySideKafkaAdapter
import shared.ports.persistence.Repository
import shared.domain.EventSourcing.CommandId
import ebikes.domain.model.*
import ebikes.ports.cqrs.EBikesQuerySide

import EBikeCommandsSerialization.given

class EBikesQuerySideKafkaAdapter(
    bootstrapServers: String,
    topic: String
) extends QuerySideKafkaAdapter[
      EBikeId,
      EBike,
      EBikeCommandErrors,
      Unit,
      EBikeCommands
    ](bootstrapServers, topic),
      EBikesQuerySide

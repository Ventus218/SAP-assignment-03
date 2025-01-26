package ebikes.adapters.cqrs

import scala.concurrent.*
import upickle.default.*
import shared.adapters.cqrs.CommandSideKafkaAdapter
import ebikes.domain.model.*
import ebikes.ports.cqrs.*

import EBikeCommandsSerialization.given

class EBikesCommandSideKafkaAdapter(
    bootstrapServers: String,
    clientId: String,
    topic: String
) extends CommandSideKafkaAdapter[
      EBikeId,
      EBike,
      EBikeCommandErrors,
      Nothing,
      EBikeCommands
    ](
      bootstrapServers,
      clientId,
      topic
    ),
      EBikesCommandSide

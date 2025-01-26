package ebikes.ports.cqrs

import ebikes.domain.model.*
import shared.ports.cqrs.CommandSide

trait EBikesCommandSide
    extends CommandSide[
      EBikeId,
      EBike,
      EBikeCommandErrors,
      Nothing,
      EBikeCommands
    ]

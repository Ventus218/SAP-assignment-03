package rides.ports.cqrs

import rides.domain.model.*
import shared.ports.cqrs.CommandSide

trait RidesCommandSide
    extends CommandSide[
      RideId,
      Ride,
      RideCommandError,
      RideCommandEnviroment,
      RideCommand
    ]

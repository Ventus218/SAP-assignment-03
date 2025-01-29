package rides.ports.cqrs

import shared.ports.cqrs.QuerySide.*
import rides.domain.model.*

trait RidesQuerySide
    extends QuerySide[
      RideId,
      Ride,
      RideCommandError,
      RideCommandEnviroment,
      RideCommand
    ]

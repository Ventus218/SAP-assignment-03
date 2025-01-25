package ebikes.ports.cqrs

import shared.ports.cqrs.QuerySide.*
import ebikes.domain.model.*

trait EBikesQuerySide
    extends QuerySide[EBikeId, EBike, EBikeCommandErrors, EBikeCommands]

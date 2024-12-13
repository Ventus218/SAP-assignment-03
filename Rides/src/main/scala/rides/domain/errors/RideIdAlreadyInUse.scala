package rides.domain.errors

import rides.domain.model.RideId

final case class RideIdAlreadyInUse(id: RideId)

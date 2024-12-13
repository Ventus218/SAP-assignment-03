package rides.domain.errors

import rides.domain.model.RideId

final case class RideNotFound(id: RideId)

package rides.domain.model;

import java.util.Date;

case class Ride(
    id: RideId,
    eBikeId: EBikeId,
    username: Username,
    start: Date,
    end: Option[Date]
)

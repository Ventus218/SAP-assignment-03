package rides.domain.model;

import java.util.Date;
import shared.domain.EventSourcing.Entity

case class Ride(
    id: RideId,
    eBikeId: EBikeId,
    username: Username,
    start: Date,
    end: Option[Date]
) extends Entity[RideId]

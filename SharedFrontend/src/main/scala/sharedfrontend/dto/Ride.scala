package sharedfrontend.dto;

import java.util.Date;
import upickle.default.*

given ReadWriter[Date] =
  readwriter[Long].bimap(
    date => date.getTime(),
    long => Date(long)
  )

case class Ride(
    id: RideId,
    eBikeId: EBikeId,
    username: Username,
    start: Date,
    end: Option[Date] = None
) derives ReadWriter

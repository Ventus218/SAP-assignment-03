package rides.domain.model

import java.util.Date

enum RideStatus:
  case BikeGoingToUser(junctionId: JunctionId)
  case UserRiding
  case BikeGoingBackToStation
  case Ended(timestamp: Date)

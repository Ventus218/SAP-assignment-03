package rides.domain.model

import java.util.Date

enum RideStatus:
  case BikeGoingToUser
  case UserRiding
  case BikeGoingBackToStation
  case Ended(timestamp: Date)

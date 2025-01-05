package ebikes.domain.model

enum EBikeEvent:
  case Registered(eBike: EBike)
  case UpdatedPhisicalData(
      id: EBikeId,
      location: Option[V2D],
      direction: Option[V2D],
      speed: Option[Double]
  )

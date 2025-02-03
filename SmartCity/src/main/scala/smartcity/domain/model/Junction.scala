package smartcity.domain.model

final case class JunctionId(value: String)
final case class Junction(
    id: JunctionId,
    hasChargingStation: Boolean,
    semaphore: Option[Semaphore],
    enteringStreets: Set[Street],
    exitingStreets: Set[Street]
)

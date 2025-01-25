package ebikes.domain.model;

import shared.domain.EventSourcing.Entity

case class EBike(id: EBikeId, location: V2D, direction: V2D, speed: Double)
    extends Entity[EBikeId]

package ebikes.domain.model;

import shared.domain.EventSourcing.Entity

case class EBike(id: EBikeId, location: EBikeLocation)
    extends Entity[EBikeId]

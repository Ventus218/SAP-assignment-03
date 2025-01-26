package rides.ports

import rides.domain.model.*

trait EBikesService:
  def exists(eBikeId: EBikeId, atTimestamp: Long = Long.MaxValue): Boolean
  def eBikes(atTimestamp: Long = Long.MaxValue): Set[EBikeId]

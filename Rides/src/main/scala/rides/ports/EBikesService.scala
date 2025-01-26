package rides.ports

import rides.domain.model.*

trait EBikesService:
  def exists(eBikeId: EBikeId): Boolean
  def eBikes(): Set[EBikeId]

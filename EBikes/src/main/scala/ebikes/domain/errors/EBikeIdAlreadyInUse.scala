package ebikes.domain.errors

import ebikes.domain.model.EBikeId

final case class EBikeIdAlreadyInUse(id: EBikeId)

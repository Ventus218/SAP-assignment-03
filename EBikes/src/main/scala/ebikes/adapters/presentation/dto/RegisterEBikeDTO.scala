package ebikes.adapters.presentation.dto

import ebikes.domain.model.*

final case class RegisterEBikeDTO(id: EBikeId, location: V2D, direction: V2D)

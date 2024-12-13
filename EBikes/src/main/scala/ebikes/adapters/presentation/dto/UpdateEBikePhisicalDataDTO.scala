package ebikes.adapters.presentation.dto

import ebikes.domain.model.*

final case class UpdateEBikePhisicalDataDTO(
    location: Option[V2D] = None,
    direction: Option[V2D] = None,
    speed: Option[Double] = None
)

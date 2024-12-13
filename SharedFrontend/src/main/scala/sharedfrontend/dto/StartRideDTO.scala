package sharedfrontend.dto

import upickle.default.*

final case class StartRideDTO(eBikeId: EBikeId, username: Username)
    derives ReadWriter

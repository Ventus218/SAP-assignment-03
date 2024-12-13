package sharedfrontend.dto;

import upickle.default.*

case class EBike(id: EBikeId, location: V2D, direction: V2D, speed: Double)
    derives ReadWriter

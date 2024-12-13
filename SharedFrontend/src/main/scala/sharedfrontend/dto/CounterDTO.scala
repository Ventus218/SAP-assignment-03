package sharedfrontend.dto

import upickle.default.*

final case class CounterDTO(id: CounterId, value: Int) derives ReadWriter

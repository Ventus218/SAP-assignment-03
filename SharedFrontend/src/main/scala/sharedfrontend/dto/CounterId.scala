package sharedfrontend.dto

import upickle.default.*

final case class CounterId(value: String) derives ReadWriter

package sharedfrontend.dto

import upickle.default.*

final case class Credit(amount: Int) derives ReadWriter

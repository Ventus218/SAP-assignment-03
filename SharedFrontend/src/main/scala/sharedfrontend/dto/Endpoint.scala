package sharedfrontend.dto

import java.net.URI
import upickle.default.*

given ReadWriter[URI] =
  readwriter[String].bimap[URI](
    uri => uri.toString(),
    string => URI(string)
  )

final case class Endpoint(value: URI) derives ReadWriter

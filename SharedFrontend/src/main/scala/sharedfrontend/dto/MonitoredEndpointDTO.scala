package sharedfrontend.dto

import upickle.default.*

final case class MonitoredEndpointDTO(
    endpoint: Endpoint,
    status: MonitoredEndpointStatus
) derives ReadWriter

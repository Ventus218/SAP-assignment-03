package sharedfrontend.dto

import upickle.default.*

enum MonitoredEndpointStatus derives ReadWriter:
  case Up
  case Down
  case Unknown

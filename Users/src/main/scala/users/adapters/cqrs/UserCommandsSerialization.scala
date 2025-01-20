package users.adapters.cqrs

import upickle.default.*
import shared.domain.EventSourcing.*
import users.domain.model.*

object UserCommandsSerialization:
  given ReadWriter[CommandId] = ReadWriter.derived
  given ReadWriter[Username] = ReadWriter.derived
  given ReadWriter[UserCommands] = ReadWriter.derived

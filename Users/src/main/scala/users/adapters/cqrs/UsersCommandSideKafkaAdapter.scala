package users.adapters.cqrs

import scala.concurrent.*
import upickle.default.*
import shared.adapters.CommandSideKafkaAdapter
import users.domain.model.*
import users.ports.cqrs.*

import UserCommandsSerialization.given

class UsersCommandSideKafkaAdapter(
    bootstrapServers: String,
    clientId: String,
    topic: String
) extends CommandSideKafkaAdapter[
      Username,
      User,
      UserCommandErrors,
      UserCommands
    ](
      bootstrapServers,
      clientId,
      topic
    ),
      UsersCommandSide

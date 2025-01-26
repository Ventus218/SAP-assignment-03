package users.adapters.cqrs

import scala.concurrent.*
import upickle.default.*
import shared.adapters.cqrs.CommandSideKafkaAdapter
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
      Nothing,
      UserCommands
    ](
      bootstrapServers,
      clientId,
      topic
    ),
      UsersCommandSide

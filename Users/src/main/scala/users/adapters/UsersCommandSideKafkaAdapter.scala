package users.adapters

import scala.concurrent.*
import upickle.default.*
import shared.adapters.CommandSideKafkaAdapter
import users.domain.model.*
import upickle.default
import users.ports.UsersCommandSide

object UsersCommandSideKafkaAdapter:
  import shared.domain.EventSourcing.CommandId
  given ReadWriter[CommandId] = ReadWriter.derived
  given ReadWriter[Username] = ReadWriter.derived
  given rw: ReadWriter[UserCommands] = ReadWriter.derived

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
    )(using UsersCommandSideKafkaAdapter.rw),
      UsersCommandSide

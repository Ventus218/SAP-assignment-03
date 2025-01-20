package users.adapters.query

import shared.adapters.QuerySideKafkaAdapter
import users.domain.model.*
import users.ports.query.UsersQuerySide
import shared.ports.persistence.Repository
import shared.domain.EventSourcing.CommandId

import users.adapters.UserCommandsSerialization.given

class UsersQuerySideKafkaAdapter(
    repo: Repository[CommandId, UserCommands],
    bootstrapServers: String,
    topic: String
) extends QuerySideKafkaAdapter[
      Username,
      User,
      UserCommandErrors,
      UserCommands
    ](repo, bootstrapServers, topic),
      UsersQuerySide

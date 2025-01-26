package users.adapters.cqrs

import shared.adapters.cqrs.QuerySideKafkaAdapter
import shared.ports.persistence.Repository
import shared.domain.EventSourcing.CommandId
import users.domain.model.*
import users.ports.cqrs.UsersQuerySide

import UserCommandsSerialization.given

class UsersQuerySideKafkaAdapter(
    bootstrapServers: String,
    topic: String
) extends QuerySideKafkaAdapter[
      Username,
      User,
      UserCommandErrors,
      Nothing,
      UserCommands
    ](bootstrapServers, topic),
      UsersQuerySide

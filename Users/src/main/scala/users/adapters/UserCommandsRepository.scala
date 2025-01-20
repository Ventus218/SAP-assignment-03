package users.adapters

import shared.domain.EventSourcing.CommandId
import shared.technologies.persistence.InMemoryMapDatabase
import shared.adapters.persistence.InMemoryRepositoryAdapter
import users.domain.model.*

import UserCommandsSerialization.given

class UserCommandsRepository(db: InMemoryMapDatabase)
    extends InMemoryRepositoryAdapter[CommandId, UserCommands](
      db,
      "user-commands"
    )

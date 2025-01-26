package users.ports.cqrs

import users.domain.model.*
import shared.ports.cqrs.CommandSide

trait UsersCommandSide
    extends CommandSide[
      Username,
      User,
      UserCommandErrors,
      Nothing,
      UserCommands
    ]

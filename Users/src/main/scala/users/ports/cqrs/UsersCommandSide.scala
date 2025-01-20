package users.ports.cqrs

import users.domain.model.*
import shared.ports.CommandSide

trait UsersCommandSide
    extends CommandSide[Username, User, UserCommandErrors, UserCommands]

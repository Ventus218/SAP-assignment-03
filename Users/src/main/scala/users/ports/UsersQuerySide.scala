package users.ports

import shared.ports.QuerySide
import users.domain.model.*

trait UsersQuerySide extends QuerySide[Username, User]

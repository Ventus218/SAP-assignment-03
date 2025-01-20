package users.ports.cqrs

import shared.ports.cqrs.QuerySide
import users.domain.model.*

trait UsersQuerySide extends QuerySide[Username, User]

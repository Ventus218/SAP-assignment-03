package users.domain.errors

import users.domain.model.Username

final case class UsernameAlreadyInUse(username: Username)

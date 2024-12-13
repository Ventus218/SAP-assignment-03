package users.domain.errors

import users.domain.model.Username

final case class UserNotFound(username: Username)

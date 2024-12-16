package users.domain;

import users.domain.model.*;
import users.domain.errors.*

trait UsersService:

  def registerUser(username: Username): Either[UsernameAlreadyInUse, User]

  def users(): Iterable[User]

  def healthCheckError(): Option[String]

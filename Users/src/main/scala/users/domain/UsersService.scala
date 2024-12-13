package users.domain;

import users.domain.model.*;
import users.domain.errors.*

trait UsersService:

  def registerUser(username: Username): Either[UsernameAlreadyInUse, User]

  def checkCredit(username: Username): Either[UserNotFound, Credit]

  def rechargeCredit(
      username: Username,
      rechargeAmount: Credit
  ): Either[UserNotFound, Credit]

  def users(): Iterable[User]

  def healthCheckError(): Option[String]

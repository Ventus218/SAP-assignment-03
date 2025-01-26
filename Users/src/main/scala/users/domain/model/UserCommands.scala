package users.domain.model

import shared.domain.EventSourcing.*

sealed trait UserCommands
    extends Command[Username, User, UserCommandErrors, Nothing]

enum UserCommandErrors:
  case UsernameAlreadyInUse(username: Username)
  case NotFound(username: Username)

object UserCommands:
  import UserCommandErrors.*
  case class Registered(
      id: CommandId,
      entityId: Username,
      timestamp: Option[Long] = None
  ) extends UserCommands:
    def apply(
        previous: Option[User],
        env: Option[Nothing] = None
    ): Either[UsernameAlreadyInUse, Option[User]] =
      previous match
        case None       => Right(Some(User(entityId)))
        case Some(user) => Left(UsernameAlreadyInUse(entityId))

  case class Delete(
      id: CommandId,
      entityId: Username,
      timestamp: Option[Long] = None
  ) extends UserCommands:
    def apply(
        previous: Option[User],
        env: Option[Nothing] = None
    ): Either[NotFound, Option[User]] =
      previous match
        case None        => Left(NotFound(entityId))
        case Some(value) => Right(None)

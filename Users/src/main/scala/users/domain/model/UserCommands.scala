package users.domain.model

import shared.domain.EventSourcing.*

sealed trait UserCommands
    extends Command[Username, User, UserCommandErrors, Unit, UserCommands]

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

    def setTimestamp(timestamp: Long): Registered =
      copy(timestamp = Some(timestamp))
    def apply(entities: Map[Username, User])(using
        Unit
    ): Either[UsernameAlreadyInUse, Map[Username, User]] =
      entities.get(entityId) match
        case None       => Right(entities.updated(entityId, User(entityId)))
        case Some(user) => Left(UsernameAlreadyInUse(entityId))

  case class Delete(
      id: CommandId,
      entityId: Username,
      timestamp: Option[Long] = None
  ) extends UserCommands:

    def setTimestamp(timestamp: Long): Delete =
      copy(timestamp = Some(timestamp))
    def apply(entities: Map[Username, User])(using
        Unit
    ): Either[NotFound, Map[Username, User]] =
      entities.get(entityId) match
        case None        => Left(NotFound(entityId))
        case Some(value) => Right(entities.removed(entityId))

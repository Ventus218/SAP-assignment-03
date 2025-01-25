package users.domain;

import scala.concurrent.*
import shared.domain.EventSourcing.*
import shared.ports.cqrs.QuerySide.Errors.*
import users.domain.model.*;
import users.ports.cqrs.*
import users.ports.UsersService

class UsersServiceImpl(
    private val usersCommandSide: UsersCommandSide,
    private val usersQuerySide: UsersQuerySide
) extends UsersService:

  override def registerUser(username: Username)(using
      ExecutionContext
  ): Future[CommandId] =
    val command = UserCommands.Registered(CommandId.random(), username)
    usersCommandSide.publish(command).map(_ => command.id)

  override def users(): Iterable[User] =
    usersQuerySide.getAll()

  override def commandResult(
      id: CommandId
  ): Either[CommandNotFound, Either[UserCommandErrors, Option[User]]] =
    usersQuerySide.commandResult(id) match
      case Left(value)  => Left(CommandNotFound(value.id))
      case Right(value) => Right(value)

  override def healthCheckError(): Option[String] = None

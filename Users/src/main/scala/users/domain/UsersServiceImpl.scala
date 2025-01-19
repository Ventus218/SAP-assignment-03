package users.domain;

import scala.concurrent.*
import shared.domain.EventSourcing.CommandId
import users.domain.model.*;
import users.domain.errors.*
import users.ports.*

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

  override def healthCheckError(): Option[String] = None

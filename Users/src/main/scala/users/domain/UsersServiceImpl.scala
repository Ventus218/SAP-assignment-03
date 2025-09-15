package users.domain;

import scala.concurrent.*
import shared.domain.EventSourcing.*
import shared.ports.cqrs.QuerySide.Errors.*
import users.domain.model.*;
import users.ports.cqrs.*
import users.ports.UsersService
import shared.Utils

class UsersServiceImpl(
    private val usersCommandSide: UsersCommandSide,
    private val usersQuerySide: UsersQuerySide
) extends UsersService:

  given Unit = ()

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
      case Left(value) => Left(CommandNotFound(value.id))
      case Right(value) =>
        val command = usersQuerySide.commands().find(_.id == id).get
        Right(value.map(_.get(command.entityId)))

  override def healthCheckError(using
      ExecutionContext
  ): Future[Option[String]] =
    for healthChecks <- Future.sequence(
        Seq(usersCommandSide.healthCheck, usersQuerySide.healthCheck)
      )
    yield (Utils.combineHealthCheckErrors(healthChecks*))

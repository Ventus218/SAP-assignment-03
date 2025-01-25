package users.ports;

import scala.concurrent.*
import users.domain.model.*;
import shared.domain.EventSourcing.*
import shared.ports.cqrs.QuerySide.Errors.*

trait UsersService:

  def registerUser(username: Username)(using
      ExecutionContext
  ): Future[CommandId]

  def users(): Iterable[User]

  def commandResult(
      id: CommandId
  ): Either[CommandNotFound, Either[UserCommandErrors, Option[User]]]

  def healthCheckError(): Option[String]

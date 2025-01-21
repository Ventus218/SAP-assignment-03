package users.ports;

import scala.concurrent.*
import users.domain.model.*;
import users.domain.errors.*
import shared.domain.EventSourcing.*

trait UsersService:

  def registerUser(username: Username)(using
      ExecutionContext
  ): Future[CommandId]

  def users(): Iterable[User]

  def commandResult(
      id: CommandId
  ): Either[CommandNotFound, Either[UserCommandErrors, Option[User]]]

  def healthCheckError(): Option[String]

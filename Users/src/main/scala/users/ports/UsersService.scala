package users.ports;

import scala.concurrent.*
import users.domain.model.*;
import users.domain.errors.*
import shared.domain.EventSourcing.CommandId

trait UsersService:

  def registerUser(username: Username)(using
      ExecutionContext
  ): Future[CommandId]

  def users(): Iterable[User]

  def healthCheckError(): Option[String]

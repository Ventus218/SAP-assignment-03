package users.domain;

import scala.concurrent.*
import users.domain.model.*;
import users.domain.errors.*

trait UsersService:

  def registerUser(username: Username)(using ec: ExecutionContext): Future[Either[UsernameAlreadyInUse, User]]

  def users()(using ec: ExecutionContext): Future[Iterable[User]]

  def healthCheckError(): Option[String]

package users.domain;

import scala.concurrent.*
import users.domain.model.*
import users.domain.errors.*
import users.ports.persistence.UsersEventStore.*

class UsersServiceImpl(private val usersEventStore: UsersEventStore)
    extends UsersService:

  override def registerUser(username: Username)(using
      ec: ExecutionContext
  ): Future[Either[UsernameAlreadyInUse, User]] =
    val user = User(username)
    for
      users <- users()
      res <-
        if users.exists(_ == user) then
          Future(Left(UsernameAlreadyInUse(username)))
        else
          usersEventStore.publish(CreateUserEvent(user)).map(_ => Right(user))
    yield (res)

  override def users()(using ec: ExecutionContext): Future[Iterable[User]] =
    usersEventStore
      .allEvents()
      .map(_.toSet)
      .map(_.map(_.user))

  override def healthCheckError(): Option[String] = None

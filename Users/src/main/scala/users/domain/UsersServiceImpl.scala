package users.domain;

import users.domain.model.*;
import users.domain.errors.*
import users.ports.persistence.UsersRepository;

class UsersServiceImpl(private val usersRepository: UsersRepository)
    extends UsersService:

  override def registerUser(
      username: Username
  ): Either[UsernameAlreadyInUse, User] =
    val user = User(username)
    usersRepository.insert(user.username, user) match
      case Left(value)  => Left(UsernameAlreadyInUse(username))
      case Right(value) => Right(user)

  override def users(): Iterable[User] =
    usersRepository.getAll()

  override def healthCheckError(): Option[String] = None

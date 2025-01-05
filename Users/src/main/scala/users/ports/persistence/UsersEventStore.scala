package users.ports.persistence

import scala.concurrent.*
import users.domain.model.*

object UsersEventStore:
  case class CreateUserEvent(user: User):
    def key: Username = user.username

  trait UsersEventStore:

    def publish(e: CreateUserEvent)(using ec: ExecutionContext): Future[Unit]
    def allEvents()(using
        ec: ExecutionContext
    ): Future[Iterable[CreateUserEvent]]

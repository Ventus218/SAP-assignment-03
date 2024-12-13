package rides.adapters.usersservice

import scala.concurrent.Future
import scala.concurrent.ExecutionContextExecutor
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.*
import akka.actor.typed.ActorSystem
import rides.ports.UsersService
import rides.domain.model.*

class UsersServiceAdapter(private val address: String)(using
    actorSystem: ActorSystem[Any]
) extends UsersService:

  given ExecutionContextExecutor = actorSystem.executionContext

  private val usersEndpoint = s"http://$address/users"

  override def exist(username: Username): Future[Boolean] =
    Http()
      .singleRequest(
        HttpRequest(uri = s"$usersEndpoint/${username.value}/credit")
      )
      .map(_.status.isSuccess)

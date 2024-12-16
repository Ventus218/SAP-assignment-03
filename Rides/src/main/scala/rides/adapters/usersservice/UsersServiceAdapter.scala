package rides.adapters.usersservice

import scala.concurrent.Future
import scala.concurrent.ExecutionContextExecutor
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.*
import akka.actor.typed.ActorSystem
import rides.ports.UsersService
import rides.domain.model.*
import rides.adapters.Marshalling.{given, *}

class UsersServiceAdapter(private val address: String)(using
    actorSystem: ActorSystem[Any]
) extends UsersService:

  given ExecutionContextExecutor = actorSystem.executionContext

  private val usersEndpoint = s"http://$address/users"

  private case class User(username: Username)
  private given RootJsonFormat[User] = jsonFormat1(User.apply)

  override def exist(username: Username): Future[Boolean] =
    for
      res <- Http().singleRequest(HttpRequest(uri = s"$usersEndpoint"))
      users <- Unmarshal(res).to[Array[User]]
    yield (users.exists(_.username == username))

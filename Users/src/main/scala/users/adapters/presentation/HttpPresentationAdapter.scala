package users.adapters.presentation

import scala.concurrent.*
import akka.actor.typed.*
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.StatusCodes.*
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.RootJsonFormat
import spray.json.DefaultJsonProtocol.*
import shared.adapters.presentation.*
import users.domain.model.*
import users.domain.UsersService

object HttpPresentationAdapter:

  given RootJsonFormat[Username] = jsonFormat1(Username.apply)
  given RootJsonFormat[User] = jsonFormat1(User.apply)
  given RootJsonFormat[HealthCheckError] = jsonFormat1(HealthCheckError.apply)

  def startHttpServer(
      usersService: UsersService,
      host: String,
      port: Int
  )(using actorSystem: ActorSystem[Any]): Future[ServerBinding] =
    // For IO bounded computations in the service
    given ExecutionContext =
      actorSystem.dispatchers.lookup(DispatcherSelector.blocking())

    val route =
      concat(
        pathPrefix("users"):
          handleExceptions(ExceptionHandlers.log):
            concat(
              (get & pathEnd & onSuccess(usersService.users())): users =>
                complete(users.toArray),
              (post & pathEnd):
                entity(as[Username]): username =>
                  onSuccess(usersService.registerUser(username)):
                    case Left(value) =>
                      complete(Conflict, "Username already in use")
                    case Right(value) => complete(value)
            )
        ,
        path("healthCheck"):
          usersService.healthCheckError() match
            case None => complete(OK, HttpEntity.Empty)
            case Some(value) =>
              complete(ServiceUnavailable, HealthCheckError(value))
      )

    Http().newServerAt(host, port).bind(route)

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
import users.domain.model.*
import users.domain.UsersService

object HttpPresentationAdapter:

  given RootJsonFormat[Username] = jsonFormat1(Username.apply)
  given RootJsonFormat[User] = jsonFormat1(User.apply)
  import shared.adapters.presentation.HealthCheckError
  given RootJsonFormat[HealthCheckError] = jsonFormat1(HealthCheckError.apply)
  import shared.domain.EventSourcing.CommandId
  given RootJsonFormat[CommandId] = jsonFormat1(CommandId.apply)

  def startHttpServer(
      usersService: UsersService,
      host: String,
      port: Int
  )(using system: ActorSystem[Any]): Future[ServerBinding] =
    // For IO bounded computations in the service
    given ExecutionContext =
      system.dispatchers.lookup(DispatcherSelector.blocking())

    val route =
      concat(
        pathPrefix("users"):
          concat(
            (get & pathEnd):
              complete(usersService.users().toArray)
            ,
            (post & pathEnd):
              entity(as[Username]): username =>
                onSuccess(usersService.registerUser(username)): commandId =>
                  complete(commandId)
          )
        ,
        path("healthCheck"):
          usersService.healthCheckError() match
            case None => complete(OK, HttpEntity.Empty)
            case Some(value) =>
              complete(ServiceUnavailable, HealthCheckError(value))
      )

    Http().newServerAt(host, port).bind(route)

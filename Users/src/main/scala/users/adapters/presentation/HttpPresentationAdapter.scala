package users.adapters.presentation

import scala.concurrent.Future
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.StatusCodes.*
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.RootJsonFormat
import spray.json.DefaultJsonProtocol.*
import shared.adapters.presentation.HealthCheckError
import users.domain.model.*
import users.domain.UsersService

object HttpPresentationAdapter:

  given RootJsonFormat[Username] = jsonFormat1(Username.apply)
  given RootJsonFormat[Credit] = jsonFormat1(Credit.apply)
  given RootJsonFormat[User] = jsonFormat2(User.apply)
  given RootJsonFormat[HealthCheckError] = jsonFormat1(HealthCheckError.apply)

  def startHttpServer(
      usersService: UsersService,
      host: String,
      port: Int
  )(using ActorSystem[Any]): Future[ServerBinding] =
    val route =
      concat(
        pathPrefix("users"):
          concat(
            (get & pathEnd):
              complete(usersService.users().toArray)
            ,
            (post & pathEnd):
              entity(as[Username]) { username =>
                usersService.registerUser(username) match
                  case Left(value) =>
                    complete(Conflict, "Username already in use")
                  case Right(value) => complete(value)
              }
            ,
            pathPrefix(Segment / "credit"): username =>
              concat(
                (get & pathEnd):
                  usersService.checkCredit(Username(username)) match
                    case Left(value)  => complete(NotFound, "User not found")
                    case Right(value) => complete(value)
                ,
                (post & pathEnd):
                  entity(as[Credit]) { credit =>
                    usersService
                      .rechargeCredit(Username(username), credit) match
                      case Left(value)  => complete(NotFound, "User not found")
                      case Right(value) => complete(value)

                  }
              )
          )
        ,
        path("healthCheck"):
          usersService.healthCheckError() match
            case None => complete(OK, HttpEntity.Empty)
            case Some(value) =>
              complete(ServiceUnavailable, HealthCheckError(value))
      )

    Http().newServerAt(host, port).bind(route)

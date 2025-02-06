package smartcity.adapters.presentation

import scala.concurrent.*
import akka.actor.typed.*
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.StatusCodes.*
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.*
import spray.json.DefaultJsonProtocol.*
import spray.json.JsonWriter.func2Writer
import spray.json.JsonReader.func2Reader
import smartcity.domain.model.*
import smartcity.ports.SmartCityService
import smartcity.ports.SmartCityService.*

object HttpPresentationAdapter:

  import upickle.default.*
  given ReadWriter[SemaphoreState] = ReadWriter.derived
  given JsonFormat[SemaphoreState] = jsonFormat[SemaphoreState](
    func2Reader(js => read[SemaphoreState](js.compactPrint)),
    func2Writer(s => write(s).parseJson)
  )
  given RootJsonFormat[JunctionId] = jsonFormat1(JunctionId.apply)
  given RootJsonFormat[Semaphore] = jsonFormat5(Semaphore.apply)
  given RootJsonFormat[StreetId] = jsonFormat1(StreetId.apply)
  given RootJsonFormat[Street] = jsonFormat2(Street.apply)
  given RootJsonFormat[Junction] = jsonFormat4(Junction.apply)
  import shared.adapters.presentation.HealthCheckError
  given RootJsonFormat[HealthCheckError] = jsonFormat1(HealthCheckError.apply)

  def startHttpServer(
      service: SmartCityService,
      host: String,
      port: Int
  )(using ActorSystem[Any]): Future[ServerBinding] =
    val route =
      concat(
        pathPrefix("smartcity"):
          concat(
            (get & path("junctions")):
              complete(service.junctions().toArray)
            ,
            (get & path("semaphores" / Segment)): segment =>
              val semaphoreId = JunctionId(segment)
              service.semaphore(semaphoreId) match
                case Left(_) =>
                  complete(NotFound, s"Semaphore $segment doesn't exist")
                case Right(value) => complete(value)
            ,
            (get & path("path")):
              parameter("from".optional, "to".optional): (from, to) =>
                (from, to) match
                  case (Some(from), Some(to)) =>
                    service.bestPath(JunctionId(from), JunctionId(to)) match
                      case Left(value) =>
                        complete(NotFound, s"Junction ${value.id} not found")
                      case Right(value) =>
                        complete(value.toArray)
                  case _ =>
                    complete(
                      BadRequest,
                      "Missing query parameters \"from\" and \"to\""
                    )
          )
        ,
        path("healthCheck"):
          service.healthCheckError() match
            case None => complete(OK, HttpEntity.Empty)
            case Some(value) =>
              complete(ServiceUnavailable, HealthCheckError(value))
      )

    Http().newServerAt(host, port).bind(route)

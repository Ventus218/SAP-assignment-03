package rides.adapters.presentation

import java.util.Date;
import scala.concurrent.Future
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.StatusCodes.*
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.*
import spray.json.JsonWriter.func2Writer
import spray.json.JsonReader.func2Reader
import spray.json.DefaultJsonProtocol.*
import shared.adapters.presentation.HealthCheckError
import shared.ports.MetricsService
import rides.domain.model.*
import rides.ports.RidesService
import rides.adapters.presentation.dto.*
import rides.domain.errors.*
import rides.domain.errors.UserOrEBikeAlreadyOnARide.*
import rides.domain.errors.UserOrEBikeDoesNotExist.*

object HttpPresentationAdapter:

  given JsonFormat[Date] = jsonFormat(
    func2Reader(js => Date(js.asInstanceOf[JsNumber].value.toLong)),
    func2Writer[Date](date => JsNumber(date.getTime()))
  )
  given RootJsonFormat[Username] = jsonFormat1(Username.apply)
  given RootJsonFormat[EBikeId] = jsonFormat1(EBikeId.apply)
  given RootJsonFormat[RideId] = jsonFormat1(RideId.apply)
  given RootJsonFormat[Ride] = jsonFormat5(Ride.apply)
  given RootJsonFormat[StartRideDTO] = jsonFormat2(StartRideDTO.apply)
  given RootJsonFormat[HealthCheckError] = jsonFormat1(HealthCheckError.apply)

  private val metricsCounterName = "rides_service_requests"

  def startHttpServer(
      ridesService: RidesService,
      host: String,
      port: Int,
      metricsService: MetricsService
  )(using ActorSystem[Any]): Future[ServerBinding] =
    val route =
      concat(
        pathPrefix("rides"):
          metricsService.incrementCounterByOne(metricsCounterName)
          concat(
            (path("active") & get):
              complete(ridesService.activeRides().toArray)
            ,
            (path("availableEBikes") & get):
              onSuccess(ridesService.availableEBikes()): availableEBikes =>
                complete(availableEBikes.toArray)
            ,
            (post & pathEnd):
              entity(as[StartRideDTO]) { dto =>
                onSuccess(ridesService.startRide(dto.eBikeId, dto.username)):
                  _ match
                    case Left(UserAlreadyOnARide(username)) =>
                      complete(
                        Conflict,
                        s"User ${username.value} already riding"
                      )
                    case Left(EBikeAlreadyOnARide(eBikeId)) =>
                      complete(
                        Conflict,
                        s"EBike ${eBikeId.value} already riding"
                      )
                    case Left(UserDoesNotExist(user)) =>
                      complete(NotFound, s"User ${user.value} does not exists")
                    case Left(EBikeDoesNotExist(id)) =>
                      complete(NotFound, s"EBike ${id.value} does not exists")
                    case Left(FailureInOtherService(msg)) =>
                      complete(InternalServerError, msg)
                    case Right(value) => complete(value)
              }
            ,
            path(Segment): rideId =>
              concat(
                get:
                  ridesService.find(RideId(rideId)) match
                    case None        => complete(NotFound, "Ride not found")
                    case Some(value) => complete(value)
                ,
                (put & pathEnd):
                  onSuccess(ridesService.endRide(RideId(rideId))):
                    _ match
                      case Left(value)  => complete(NotFound, "Ride not found")
                      case Right(value) => complete(value)
              )
          )
        ,
        path("healthCheck"):
          ridesService.healthCheckError() match
            case None => complete(OK, HttpEntity.Empty)
            case Some(value) =>
              complete(ServiceUnavailable, HealthCheckError(value))
      )

    Http().newServerAt(host, port).bind(route)

package rides.adapters.presentation

import scala.concurrent.*
import akka.actor.typed.*
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.StatusCodes.*
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.server.Directives._
import shared.adapters.presentation.*
import shared.domain.EventSourcing.CommandId
import rides.domain.model.*
import rides.ports.RidesService
import rides.adapters.presentation.dto.*
import rides.domain.model.RideCommandError.*

object HttpPresentationAdapter:
  import Serialization.{*, given}

  def startHttpServer(
      ridesService: RidesService,
      host: String,
      port: Int
  )(using system: ActorSystem[Any]): Future[ServerBinding] =
    // For IO bounded computations in the service
    given ExecutionContext =
      system.dispatchers.lookup(DispatcherSelector.blocking())

    val route =
      handleExceptions(ExceptionHandlers.log):
        concat(
          pathPrefix("rides"):
            concat(
              (path("active") & get):
                complete(ridesService.activeRides().toArray)
              ,
              (path("availableEBikes") & get):
                complete(ridesService.availableEBikes().toArray)
              ,
              (post & pathEnd):
                entity(as[StartRideDTO]): dto =>
                  onSuccess(ridesService.startRide(dto.eBikeId, dto.username)):
                    complete(_)
              ,
              path(Segment): segment =>
                val rideId = RideId(segment)
                concat(
                  get:
                    ridesService.find(rideId) match
                      case None        => complete(NotFound, "Ride not found")
                      case Some(value) => complete(value)
                  ,
                  (put & pathEnd):
                    onSuccess(ridesService.endRide(rideId)):
                      complete(_)
                )
              ,
              (get & path("commands" / Segment)): segment =>
                ridesService.commandResult(CommandId(segment)) match
                  case Left(value) => complete(NotFound)
                  case Right(value) =>
                    value match
                      case Right(value) => complete(value)
                      case Left(value) =>
                        value match
                          case RideNotFound(id) =>
                            complete(NotFound, s"Ride ${id.value} not found")
                          case EBikeNotFound(id) =>
                            complete(NotFound, s"EBike ${id.value} not found")
                          case UserNotFound(id) =>
                            complete(NotFound, s"User ${id.value} not found")
                          case EBikeAlreadyInUse(id) =>
                            complete(
                              Conflict,
                              s"EBike ${id.value} already used"
                            )
                          case UserAlreadyRiding(id) =>
                            complete(
                              Conflict,
                              s"User ${id.value} already riding"
                            )
                          case RideAlreadyEnded(id) =>
                            complete(
                              Conflict,
                              s"Ride ${id.value} already ended"
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

private object Serialization:
  import scala.util.Try
  import java.util.Date;
  import spray.json.*
  import spray.json.JsonWriter.func2Writer
  import spray.json.JsonReader.func2Reader
  export spray.json.DefaultJsonProtocol.*
  export akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  given JsonFormat[Date] = jsonFormat(
    func2Reader(js => Date(js.asInstanceOf[JsNumber].value.toLong)),
    func2Writer[Date](date => JsNumber(date.getTime()))
  )
  import upickle.default.*
  given ReadWriter[Date] =
    readwriter[Long].bimap(
      date => date.getTime(),
      long => Date(long)
    )
  given ReadWriter[RideStatus] = ReadWriter.derived
  given JsonFormat[RideStatus] = jsonFormat[RideStatus](
    func2Reader(js => read[RideStatus](js.compactPrint)),
    func2Writer(s => write(s).parseJson)
  )
  given RootJsonFormat[Username] = jsonFormat1(Username.apply)
  given RootJsonFormat[EBikeId] = jsonFormat1(EBikeId.apply)
  given RootJsonFormat[RideId] = jsonFormat1(RideId.apply)
  given RootJsonFormat[Ride] = jsonFormat5(Ride.apply)
  given RootJsonFormat[StartRideDTO] = jsonFormat2(StartRideDTO.apply)
  given RootJsonFormat[HealthCheckError] = jsonFormat1(HealthCheckError.apply)
  given RootJsonFormat[CommandId] = jsonFormat1(CommandId.apply)

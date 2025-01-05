package ebikes.adapters.presentation

import scala.concurrent.*
import akka.actor.typed.*
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.StatusCodes.*
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.RootJsonFormat
import spray.json.DefaultJsonProtocol.*
import ebikes.domain.model.*
import ebikes.domain.EBikesService
import ebikes.adapters.presentation.dto.*
import shared.adapters.presentation.HealthCheckError

object HttpPresentationAdapter:

  given RootJsonFormat[V2D] = jsonFormat2(V2D.apply)
  given RootJsonFormat[EBikeId] = jsonFormat1(EBikeId.apply)
  given RootJsonFormat[EBike] = jsonFormat4(EBike.apply)
  given RootJsonFormat[RegisterEBikeDTO] = jsonFormat3(RegisterEBikeDTO.apply)
  given RootJsonFormat[UpdateEBikePhisicalDataDTO] = jsonFormat3(
    UpdateEBikePhisicalDataDTO.apply
  )
  given RootJsonFormat[HealthCheckError] = jsonFormat1(HealthCheckError.apply)

  def startHttpServer(
      eBikesService: EBikesService,
      host: String,
      port: Int
  )(using actorSystem: ActorSystem[Any]): Future[ServerBinding] =
    // For IO bounded computations in the service
    given ExecutionContext =
      actorSystem.dispatchers.lookup(DispatcherSelector.blocking())

    val route =
      concat(
        pathPrefix("ebikes"):
          concat(
            (get & pathEnd & onSuccess(eBikesService.eBikes())): eBikes =>
              complete(eBikes.toArray),
            (post & pathEnd):
              entity(as[RegisterEBikeDTO]) { dto =>
                onSuccess(
                  eBikesService.register(dto.id, dto.location, dto.direction)
                ):
                  _ match
                    case Left(value) =>
                      complete(Conflict, "EBike id already in use")
                    case Right(value) => complete(value)
              }
            ,
            pathPrefix(Segment): segment =>
              val eBikeId = EBikeId(segment)
              concat(
                (get & pathEnd & onSuccess(eBikesService.find(eBikeId))):
                  _ match
                    case None        => complete(NotFound, "EBike not found")
                    case Some(value) => complete(value)
                ,
                (patch & pathEnd):
                  entity(as[UpdateEBikePhisicalDataDTO]): dto =>
                    onSuccess(
                      eBikesService.updatePhisicalData(
                        eBikeId,
                        dto.location,
                        dto.direction,
                        dto.speed
                      )
                    ):
                      _ match
                        case None =>
                          complete(NotFound, s"EBike $segment not found")
                        case Some(eBike) => complete(eBike)
              )
          )
        ,
        path("healthCheck"):
          eBikesService.healthCheckError() match
            case None => complete(OK, HttpEntity.Empty)
            case Some(value) =>
              complete(ServiceUnavailable, HealthCheckError(value))
      )

    Http().newServerAt(host, port).bind(route)

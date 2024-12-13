package ebikes.adapters.presentation

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.*
import scala.concurrent.*
import scala.concurrent.duration.Duration
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.*
import akka.http.scaladsl.model.StatusCodes.*
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import spray.json.DefaultJsonProtocol.*
import ebikes.domain.model.*
import ebikes.domain.EBikesService
import ebikes.domain.errors.EBikeIdAlreadyInUse
import ebikes.adapters.presentation.HttpPresentationAdapter.{*, given}
import ebikes.adapters.presentation.dto.RegisterEBikeDTO
import shared.ports.MetricsService

class HttpPresentationAdapterTests extends AnyFlatSpec:
  "GET /ebikes" should "return all ebikes from the service" in new MockServiceEnv:
    val bikes = Await.result(
      for
        response <- Http().singleRequest(HttpRequest(uri = uri + "/ebikes"))
        bikes <- Unmarshal(response).to[Array[EBike]]
      yield (bikes),
      timeout
    )
    bikes should contain theSameElementsAs service.eBikes()

  "GET /ebikes/:id" should "return the bike if found" in new MockServiceEnv:
    val searchedBike = service.eBikes().head
    val bike = Await.result(
      for
        response <- Http().singleRequest(
          HttpRequest(uri = uri + "/ebikes/" + searchedBike.id.value)
        )
        bike <- Unmarshal(response).to[EBike]
      yield (bike),
      timeout
    )
    bike shouldBe searchedBike

  it should "return error 404 if the bike is not found" in new MockServiceEnv:
    val response = Await.result(
      Http().singleRequest(HttpRequest(uri = uri + "/ebikes/blabla")),
      timeout
    )
    response.status shouldBe NotFound

  "POST /ebikes" should "register a new bike" in new MockServiceEnv:
    val dto = RegisterEBikeDTO(EBikeId("b3"), V2D(), V2D())
    val createdBike = Await.result(
      for
        body <- Marshal(dto).to[MessageEntity]
        res <- Http().singleRequest(
          HttpRequest(
            method = HttpMethods.POST,
            uri = uri + "/ebikes",
            entity = body
          )
        )
        bike <- Unmarshal(res).to[EBike]
      yield (bike),
      timeout
    )
    createdBike shouldBe EBike(dto.id, dto.location, dto.direction, 0)

  it should "return error 409 if the bike id already exists" in new MockServiceEnv:
    val dto = RegisterEBikeDTO(service.eBikes().head.id, V2D(), V2D())
    val response = Await.result(
      for
        body <- Marshal(dto).to[MessageEntity]
        res <- Http().singleRequest(
          HttpRequest(
            method = HttpMethods.POST,
            uri = uri + "/ebikes",
            entity = body
          )
        )
      yield (res),
      timeout
    )
    response.status shouldBe Conflict

  trait MockServiceEnv:
    val service = new EBikesService:

      private var bikes = Set(
        EBike(EBikeId("b1"), V2D(), V2D(), 0),
        EBike(EBikeId("b2"), V2D(), V2D(), 0)
      )
      override def find(id: EBikeId): Option[EBike] = bikes.find(_.id == id)

      override def eBikes(): Iterable[EBike] = bikes

      override def register(
          id: EBikeId,
          location: V2D,
          direction: V2D
      ): Either[EBikeIdAlreadyInUse, EBike] =
        bikes.find(_.id == id) match
          case None =>
            val bike = EBike(id, location, direction, 0)
            bikes = bikes + bike
            Right(bike)
          case Some(value) => Left(EBikeIdAlreadyInUse(id))

      override def updatePhisicalData(
          eBikeId: EBikeId,
          location: Option[V2D],
          direction: Option[V2D],
          speed: Option[Double]
      ): Option[EBike] =
        bikes = bikes.collect({ case EBike(`eBikeId`, loc, dir, s) =>
          EBike(
            eBikeId,
            location.getOrElse(loc),
            direction.getOrElse(dir),
            speed.getOrElse(s)
          )
        })
        bikes.find(_.id == eBikeId)

      override def healthCheckError(): Option[String] = None

    val stubMetricsService = new MetricsService {
      def incrementCounter(counterId: String, amount: Long): Future[Unit] =
        Future.unit
      def registerForHealthcheckMonitoring(selfAddress: String): Future[Unit] =
        Future.unit
    }

    val timeout = Duration(2, "second")
    val serverBinding = Await.result(
      startHttpServer(service, "localhost", 0, stubMetricsService),
      atMost = timeout
    )
    val uri = s"http://${serverBinding.localAddress
        .getHostName()}:${serverBinding.localAddress.getPort()}"
    protected given system: ActorSystem[Any] =
      ActorSystem(Behaviors.empty, "test-actor-system")
    protected given ExecutionContextExecutor = system.executionContext

package ebikes

import scala.concurrent.*
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContextExecutor
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.BeforeAndAfterEach
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.*
import akka.http.scaladsl.model.StatusCodes.*
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol.*
import ebikes.domain.model.*
import ebikes.adapters.presentation.HttpPresentationAdapter.{given}
import ebikes.adapters.presentation.dto.RegisterEBikeDTO
import better.files._
import File._

class EBikesComponentTests extends AnyFlatSpec with BeforeAndAfterEach:
  given system: ActorSystem[Any] = ActorSystem(Behaviors.empty, "actor-system")
  given ExecutionContextExecutor = system.executionContext
  val timeout = Duration(2, "second")
  var serverBinding: Option[ServerBinding] = None
  def uri: String = serverBinding.map(_.localAddress) match
    case None       => ""
    case Some(addr) => s"http://${addr.getHostName()}:${addr.getPort()}"

  override protected def beforeEach(): Unit =
    val tempDir = File.temporaryDirectory().get()
    serverBinding = Some(
      Await.result(
        EBikes.run(
          tempDir.toJava,
          "localhost",
          0,
          "localhost:8080",
          "localhost:8081"
        ),
        timeout
      )
    )

  override protected def afterEach(): Unit =
    serverBinding match
      case None        => ()
      case Some(value) => Await.result(value.unbind(), timeout)

  "GET /ebikes" should "return no bikes initially" in:
    val bikes = Await.result(
      for
        response <- Http().singleRequest(HttpRequest(uri = uri + "/ebikes"))
        bikes <- Unmarshal(response).to[Array[EBike]]
      yield (bikes),
      timeout
    )
    bikes.isEmpty shouldBe true

  it should "return all bikes" in:
    val bikes = Await.result(
      for
        postB1 <- postEBikeRequest("b1")
        _ <- Http().singleRequest(postB1)
        postB2 <- postEBikeRequest("b2")
        _ <- Http().singleRequest(postB2)
        response <- Http().singleRequest(HttpRequest(uri = uri + "/ebikes"))
        bikes <- Unmarshal(response).to[Array[EBike]]
      yield (bikes),
      timeout
    )
    bikes.map(_.id.value) should contain theSameElementsAs Set("b1", "b2")

  "GET /ebikes/:id" should "return the bike if found" in:
    val bike = Await.result(
      for
        postB1 <- postEBikeRequest("b1")
        _ <- Http().singleRequest(postB1)
        response <- Http().singleRequest(
          HttpRequest(uri = uri + "/ebikes/" + "b1")
        )
        bike <- Unmarshal(response).to[EBike]
      yield (bike),
      timeout
    )
    bike.id.value shouldBe "b1"

  it should "return error 404 if the bike is not found" in:
    val response = Await.result(
      Http().singleRequest(HttpRequest(uri = uri + "/ebikes/blabla")),
      timeout
    )
    response.status shouldBe NotFound

  "POST /ebikes" should "register a new bike" in:
    val createdBike = Await.result(
      for
        postB1 <- postEBikeRequest("b1")
        res <- Http().singleRequest(postB1)
        bike <- Unmarshal(res).to[EBike]
      yield (bike),
      timeout
    )
    createdBike.id.value shouldBe "b1"

  it should "return error 409 if the bike id already exists" in:
    val response = Await.result(
      for
        postB1 <- postEBikeRequest("b1")
        _ <- Http().singleRequest(postB1)
        res <- Http().singleRequest(postB1)
      yield (res),
      timeout
    )
    response.status shouldBe Conflict

  private def postEBikeRequest(id: String): Future[HttpRequest] =
    val dto = RegisterEBikeDTO(EBikeId(id), V2D(), V2D())
    for
      body <- Marshal(dto).to[MessageEntity]
      req = HttpRequest(
        method = HttpMethods.POST,
        uri = uri + "/ebikes",
        entity = body
      )
    yield (req)

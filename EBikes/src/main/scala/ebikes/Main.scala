package ebikes

import java.io.File
import scala.util.Try
import scala.concurrent.ExecutionContextExecutor
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import ebikes.domain.EBikesServiceImpl
import ebikes.adapters.cqrs.*
import ebikes.adapters.presentation.HttpPresentationAdapter

object Main extends App:
  given actorSystem: ActorSystem[Any] =
    ActorSystem(Behaviors.empty, "actor-system")
  given ExecutionContextExecutor = actorSystem.executionContext

  val dbDir = File("/data/db")
  val host = sys.env.get("HOST").getOrElse("0.0.0.0")
  val port = (for
    portString <- sys.env.get("PORT")
    portInt <- Try(Option(portString.toInt)).getOrElse({
      sys.error("PORT must be an integer"); None
    })
  yield (portInt)).getOrElse(8080)

  val ebikesServiceAddress = sys.env.get("EBIKES_SERVICE_ADDRESS").get

  val kafkaBootstrapServers =
    sys.env.get("KAFKA_SERVICE_ADDRESS").getOrElse("kafka:9092")
  val kafkaTopic = "ebikes"
  val kafkaCommandSideAdapter = EBikesCommandSideKafkaAdapter(
    kafkaBootstrapServers,
    "EBikesService",
    kafkaTopic
  )
  val kafkaQuerySideAdapter =
    EBikesQuerySideKafkaAdapter(kafkaBootstrapServers, kafkaTopic)
  val eBikesService =
    EBikesServiceImpl(kafkaCommandSideAdapter, kafkaQuerySideAdapter)

  HttpPresentationAdapter
    .startHttpServer(eBikesService, host, port)
    .map(_ => println(s"EBikes is listening on $host:$port"))
    .map(_ =>
      val ridesServiceAddress = sys.env.get("RIDES_SERVICE_ADDRESS").get
      Thread.ofVirtual.start(ABikesEmulator(ridesServiceAddress))
    )

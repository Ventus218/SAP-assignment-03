package rides

import java.io.File
import scala.sys
import scala.util.Try
import scala.concurrent.ExecutionContextExecutor
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import shared.technologies.persistence.FileSystemDatabaseImpl
import rides.domain.model.*
import rides.domain.RidesServiceImpl
import rides.adapters.presentation.HttpPresentationAdapter
import rides.adapters.ebikesservice.EBikesServiceKafkaAdapter
import rides.adapters.usersservice.UsersServiceKafkaAdapter
import rides.adapters.persistence.RidesFileSystemRepositoryAdapter
import rides.adapters.cqrs.*

object Main extends App:
  given actorSystem: ActorSystem[Any] =
    ActorSystem(Behaviors.empty, "actor-system")
  given ExecutionContextExecutor = actorSystem.executionContext

  val db = FileSystemDatabaseImpl(File("/data/db"))
  val adapter = RidesFileSystemRepositoryAdapter(db)
  val kafkaBootstrapServers =
    sys.env.get("KAFKA_SERVICE_ADDRESS").getOrElse("kafka:9092")
  val kafkaTopic = "rides"
  val eBikesService = EBikesServiceKafkaAdapter(kafkaBootstrapServers, "ebikes")
  val usersService = UsersServiceKafkaAdapter(kafkaBootstrapServers, "users")
  val commandSide = RidesCommandSideKafkaAdapter(kafkaBootstrapServers, "RidesService", kafkaTopic)
  val querySide = RidesQuerySideKafkaAdapter(kafkaBootstrapServers, kafkaTopic)
  val ridesService = RidesServiceImpl(adapter, eBikesService, usersService, commandSide, querySide)
  val host = sys.env.get("HOST").getOrElse("0.0.0.0")
  val port = (for
    portString <- sys.env.get("PORT")
    portInt <- Try(Option(portString.toInt)).getOrElse({
      sys.error("PORT must be an integer"); None
    })
  yield (portInt)).getOrElse(8080)

  HttpPresentationAdapter
    .startHttpServer(ridesService, host, port)
    .map(_ => println(s"Rides is listening on $host:$port"))

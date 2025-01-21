package users

import java.io.File
import scala.sys
import scala.util.Try
import scala.concurrent.ExecutionContextExecutor
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import users.domain.model.*
import users.domain.UsersServiceImpl
import users.adapters.presentation.HttpPresentationAdapter
import users.adapters.*
import users.adapters.cqrs.*

object Main extends App:
  given actorSystem: ActorSystem[Any] =
    ActorSystem(Behaviors.empty, "actor-system")
  given ExecutionContextExecutor = actorSystem.executionContext

  val kafkaBootstrapServers =
    sys.env.get("KAFKA_SERVICE_ADDRESS").getOrElse("kafka:9092")
  val kafkaTopic = "users"
  val commandSideAdapter =
    UsersCommandSideKafkaAdapter(
      kafkaBootstrapServers,
      "UsersService",
      kafkaTopic
    )
  val querySideAdapter = UsersQuerySideKafkaAdapter(
    kafkaBootstrapServers,
    kafkaTopic
  )
  val usersService = UsersServiceImpl(commandSideAdapter, querySideAdapter)
  val host = sys.env.get("HOST").getOrElse("0.0.0.0")
  val port = (for
    portString <- sys.env.get("PORT")
    portInt <- Try(Option(portString.toInt)).getOrElse({
      sys.error("PORT must be an integer"); None
    })
  yield (portInt)).getOrElse(8080)

  HttpPresentationAdapter
    .startHttpServer(usersService, host, port)
    .map(_ => println(s"Users is listening on $host:$port"))

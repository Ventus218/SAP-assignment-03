package users

import java.io.File
import scala.sys
import scala.util.Try
import scala.concurrent.ExecutionContextExecutor
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import shared.technologies.persistence.FileSystemDatabaseImpl
import users.domain.model.*
import users.domain.UsersServiceImpl
import users.adapters.presentation.HttpPresentationAdapter
import users.adapters.persistence.UsersFileSystemRepositoryAdapter
import shared.adapters.MetricsServiceAdapter

object Main extends App:
  given actorSystem: ActorSystem[Any] =
    ActorSystem(Behaviors.empty, "actor-system")
  given ExecutionContextExecutor = actorSystem.executionContext

  val db = FileSystemDatabaseImpl(File("/data/db"))
  val adapter = UsersFileSystemRepositoryAdapter(db)
  val usersService = UsersServiceImpl(adapter)
  val metricsServiceAddress =
    sys.env.get("METRICS_SERVICE_ADDRESS").getOrElse("localhost:8080")
  val metricsService = MetricsServiceAdapter(metricsServiceAddress)
  val host = sys.env.get("HOST").getOrElse("0.0.0.0")
  val port = (for
    portString <- sys.env.get("PORT")
    portInt <- Try(Option(portString.toInt)).getOrElse({
      sys.error("PORT must be an integer"); None
    })
  yield (portInt)).getOrElse(8080)

  HttpPresentationAdapter
    .startHttpServer(usersService, host, port, metricsService)
    .map(_ => println(s"Users is listening on $host:$port"))
    .map(_ =>
      metricsService.registerForHealthcheckMonitoring(
        sys.env.get("USERS_SERVICE_ADDRESS").get
      )
    )

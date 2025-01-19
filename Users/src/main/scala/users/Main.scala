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
import users.adapters.UsersCommandSideKafkaAdapter

object Main extends App:
  given actorSystem: ActorSystem[Any] =
    ActorSystem(Behaviors.empty, "actor-system")
  given ExecutionContextExecutor = actorSystem.executionContext

  val db = FileSystemDatabaseImpl(File("/data/db"))
  val repoAdapter = UsersFileSystemRepositoryAdapter(db)
  val commandSideAdapter =
    UsersCommandSideKafkaAdapter("kafka:9092", "UsersService", "users")
  val usersService = UsersServiceImpl(commandSideAdapter, ???)
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

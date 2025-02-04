package smartcity

import scala.util.Try
import akka.actor.typed.*
import akka.actor.typed.scaladsl.Behaviors
import smartcity.domain.SmartCityServiceImpl
import smartcity.adapters.presentation.HttpPresentationAdapter

object Main extends App:
  given actorSystem: ActorSystem[Any] =
    ActorSystem(Behaviors.empty, "actor-system")
  val service = SmartCityServiceImpl()

  val host = sys.env.get("HOST").getOrElse("0.0.0.0")
  val port = (for
    portString <- sys.env.get("PORT")
    portInt <- Try(Option(portString.toInt)).getOrElse({
      sys.error("PORT must be an integer"); None
    })
  yield (portInt)).getOrElse(8080)

  HttpPresentationAdapter
    .startHttpServer(service, host, port)
    .map(_ => println(s"SmartCity is listening on $host:$port"))(using
      actorSystem.executionContext
    )

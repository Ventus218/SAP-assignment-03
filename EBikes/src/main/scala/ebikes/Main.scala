package ebikes

import java.io.File
import scala.util.Try
import scala.concurrent.ExecutionContextExecutor
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors

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

  val metricsServiceAddress =
    sys.env.get("METRICS_SERVICE_ADDRESS").getOrElse("localhost:8080")

  val ebikesServiceAddress = sys.env.get("EBIKES_SERVICE_ADDRESS").get

  EBikes
    .run(dbDir, host, port, ebikesServiceAddress, metricsServiceAddress)
    .map(_ => println(s"EBikes is listening on $host:$port"))

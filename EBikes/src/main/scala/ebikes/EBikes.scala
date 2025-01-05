package ebikes

import java.io.File
import scala.concurrent.Future
import ebikes.domain.EBikesServiceImpl
import ebikes.adapters.presentation.HttpPresentationAdapter
import ebikes.adapters.persistence.KafkaEBikesEventStoreAdapter
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http.ServerBinding

object EBikes:
  def run(
      dbDir: File,
      host: String,
      port: Int,
      eBikesServiceAddress: String
  )(using
      ActorSystem[Any]
  ): Future[ServerBinding] =
    val adapter = KafkaEBikesEventStoreAdapter(
      "ebikes-es:9092"
    ) // TODO: externalize config
    val eBikesService = EBikesServiceImpl(adapter)

    HttpPresentationAdapter
      .startHttpServer(eBikesService, host, port)

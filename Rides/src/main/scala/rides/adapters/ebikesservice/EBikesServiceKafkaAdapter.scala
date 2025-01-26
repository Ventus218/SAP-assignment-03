package rides.adapters.ebikesservice

import scala.concurrent.*
import scala.jdk.CollectionConverters.*
import upickle.default.*
import shared.technologies.Kafka.Consumer
import rides.domain.model.*
import rides.ports.EBikesService

class EBikesServiceKafkaAdapter(bootstrapServers: String, topic: String)
    extends EBikesService:

  import shared.domain.EventSourcing.CommandId
  given ReadWriter[CommandId] = ReadWriter.derived
  given ReadWriter[EBikeId] = ReadWriter.derived
  // This message definition is trimmed to the bare minimum
  private case class Register(id: CommandId, entityId: EBikeId)
      derives ReadWriter

  private var _existingEBikes = Set[EBikeId]()
  private def existingEBikes = synchronized(_existingEBikes)
  private def existingEBikes_=(v: Set[EBikeId]) = synchronized {
    _existingEBikes = v
  }

  Thread.ofVirtual
    .name("ebikes-service-consumer")
    .start(() => {
      Consumer.autocloseable(bootstrapServers): consumer =>
        consumer.subscribe(List(topic).asJava)
        while true do
          val newBikes = Iterator
            .continually(
              consumer.poll(java.time.Duration.ofMillis(20)).asScala
            )
            .takeWhile(_.nonEmpty)
            .flatten
            .map(r => read[Register](r.value()))
            .map(_.entityId)
            .toSeq
          if !newBikes.isEmpty then existingEBikes = existingEBikes ++ newBikes
    })

  def exists(eBikeId: EBikeId): Boolean =
    existingEBikes(eBikeId)

  def eBikes(): Set[EBikeId] = existingEBikes

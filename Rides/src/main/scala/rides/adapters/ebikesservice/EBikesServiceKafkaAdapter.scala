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
  private case class Register(
      id: CommandId,
      entityId: EBikeId,
      timestamp: Option[Long]
  ) derives ReadWriter

  private var _cache = Seq[Register]()
  private def cache = synchronized(_cache)
  private def cache_=(v: Seq[Register]) = synchronized { _cache = v }

  Thread.ofVirtual
    .name("ebikes-service-consumer")
    .start(() => {
      Consumer.autocloseable(bootstrapServers): consumer =>
        consumer.subscribe(List(topic).asJava)
        while true do
          val commands = Iterator
            .continually(
              consumer.poll(java.time.Duration.ofMillis(20)).asScala
            )
            .takeWhile(_.nonEmpty)
            .flatten
            .map(r => read[Register](r.value()))
            .toSeq
          if !commands.isEmpty then cache = cache ++ commands
    })

  def exists(eBikeId: EBikeId, atTimestamp: Long): Boolean =
    cache
      .takeWhile(_.timestamp.get <= atTimestamp)
      .map(_.entityId)
      .toSet(eBikeId)

  def eBikes(atTimestamp: Long): Set[EBikeId] =
    cache
      .takeWhile(_.timestamp.get <= atTimestamp)
      .map(_.entityId)
      .toSet

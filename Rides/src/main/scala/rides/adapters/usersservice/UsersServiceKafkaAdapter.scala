package rides.adapters.usersservice

import scala.concurrent.*
import scala.jdk.CollectionConverters.*
import upickle.default.*
import shared.technologies.Kafka.Consumer
import rides.domain.model.*
import rides.ports.UsersService

class UsersServiceKafkaAdapter(bootstrapServers: String, topic: String)
    extends UsersService:

  import shared.domain.EventSourcing.CommandId
  given ReadWriter[CommandId] = ReadWriter.derived
  given ReadWriter[Username] = ReadWriter.derived
  // This message definition is trimmed to the bare minimum
  private case class Registered(
      id: CommandId,
      entityId: Username,
      timestamp: Option[Long]
  ) derives ReadWriter

  private var _cache = Seq[Registered]()
  private def cache = synchronized(_cache)
  private def cache_=(v: Seq[Registered]) = synchronized { _cache = v }

  Thread.ofVirtual
    .name("users-service-consumer")
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
            .map(r => read[Registered](r.value()))
            .toSeq
          if !commands.isEmpty then cache = cache ++ commands
    })

  override def exists(username: Username, atTimestamp: Long): Boolean =
    cache
      .takeWhile(_.timestamp.get <= atTimestamp)
      .map(_.entityId)
      .toSet(username)

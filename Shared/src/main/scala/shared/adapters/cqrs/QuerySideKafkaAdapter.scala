package shared.adapters.cqrs

import scala.jdk.CollectionConverters.*
import java.time.Duration
import upickle.default.*
import shared.ports.cqrs.QuerySide.*
import shared.domain.EventSourcing.*
import shared.ports.persistence.Repository
import shared.technologies.Kafka.Consumer

class QuerySideKafkaAdapter[TId, T <: Entity[
  TId
], Error, C <: Command[
  TId,
  T,
  Error
]](bootstrapServers: String, topic: String)(using
    ReadWriter[C]
) extends QuerySide[TId, T, Error, C]:

  private var _cache = IndexedSeq[C]()
  private def cache = synchronized(_cache)
  private def cache_=(v: IndexedSeq[C]) = synchronized { _cache = v }

  Thread(() => {
    Consumer.autocloseable(bootstrapServers): consumer =>
      consumer.subscribe(List(topic).asJava)
      while true do
        val commands = Iterator
          .continually(
            consumer.poll(java.time.Duration.ofMillis(20)).asScala
          )
          .takeWhile(_.nonEmpty)
          .flatten
          .map(r => read[C](r.value()))
          .toSeq
        if !commands.isEmpty then cache = cache ++ commands
  }).start()

  override def find(id: TId): Option[T] =
    cache.filter(_.entityId == id).applyCommands()

  override def getAll(): Iterable[T] =
    cache
      .groupBy(_.entityId)
      .map((id, commands) => commands.applyCommands())
      .flatMap(_.toList)

  override def commands(): Iterable[C] =
    cache

  override def commandResult(
      id: CommandId
  ): Either[Errors.CommandNotFound, Either[Error, Option[T]]] =
    val commands = cache
    commands.find(_.id == id) match
      case None => Left(Errors.CommandNotFound(id))
      case Some(c) =>
        val previous = commands.takeWhile(_.id != id)
        Right(c(previous.applyCommands()))

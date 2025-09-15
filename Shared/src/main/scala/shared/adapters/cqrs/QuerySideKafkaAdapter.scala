package shared.adapters.cqrs

import scala.jdk.CollectionConverters.*
import java.time.Duration
import upickle.default.*
import shared.ports.cqrs.QuerySide.*
import shared.domain.EventSourcing.*
import shared.technologies.Kafka.Consumer
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class QuerySideKafkaAdapter[TId, T <: Entity[
  TId
], Error, Env, C <: Command[
  TId,
  T,
  Error,
  Env,
  C
]](bootstrapServers: String, topic: String)(using
    ReadWriter[C]
) extends QuerySide[TId, T, Error, Env, C]:

  private var _cache = IndexedSeq[C]()
  private def cache = synchronized(_cache)
  private def cache_=(v: IndexedSeq[C]) = synchronized { _cache = v }

  Thread.ofVirtual
    .name("query-side-kafka-consumer")
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
            .map(r => read[C](r.value()).setTimestamp(r.timestamp()))
            .toSeq
          if !commands.isEmpty then cache = cache ++ commands
    })

  override def find(id: TId, atTimestamp: Long)(using Env): Option[T] =
    cache
      .takeWhile(_.timestamp.get <= atTimestamp)
      .applyCommands()
      .get(id)

  override def getAll(atTimestamp: Long)(using Env): Iterable[T] =
    cache
      .takeWhile(_.timestamp.get <= atTimestamp)
      .applyCommands()
      .values

  override def commands(atTimestamp: Long): Iterable[C] =
    cache
      .takeWhile(_.timestamp.get <= atTimestamp)

  override def commandResult(id: CommandId)(using
      Env
  ): Either[Errors.CommandNotFound, Either[Error, Map[TId, T]]] =
    val commands = cache
    commands.find(_.id == id) match
      case None => Left(Errors.CommandNotFound(id))
      case Some(c) =>
        val previousState = commands.takeWhile(_.id != id).applyCommands()
        Right(c(previousState))

  import shared.technologies.Kafka
  import shared.technologies.Kafka.Reachable.*
  override def healthCheck(using ExecutionContext): Future[Option[String]] =
    for isReachable <- Kafka.isReachable(bootstrapServers)
    yield (isReachable match
      case Reachable        => None
      case Unreachable(err) => Some(err)
    )

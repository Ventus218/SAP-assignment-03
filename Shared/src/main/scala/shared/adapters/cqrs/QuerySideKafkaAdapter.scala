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
]](repo: Repository[CommandId, C], bootstrapServers: String, topic: String)(
    using ReadWriter[C]
) extends QuerySide[TId, T, Error, C]:

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
        if !commands.isEmpty then
          repo.transaction:
            commands.foreach(c =>
              repo.insert(c.id, c) match
                case Right(value) => ()
                // should not happen if ids are created as expected
                case Left(value) => throw value
            )
  }).start()

  override def find(id: TId): Option[T] =
    repo.getAll().filter(_.entityId == id).applyCommands()

  override def getAll(): Iterable[T] =
    repo
      .getAll()
      .groupBy(_.entityId)
      .map((id, commands) => commands.applyCommands())
      .flatMap(_.toList)

  override def commands(): Iterable[C] =
    repo.getAll()

  override def commandResult(
      id: CommandId
  ): Either[Errors.CommandNotFound, Either[Error, Option[T]]] =
    val commands = repo.getAll()
    commands.find(_.id == id) match
      case None => Left(Errors.CommandNotFound(id))
      case Some(c) =>
        val previous = commands.takeWhile(_.id != id)
        Right(c(previous.applyCommands()))

package shared.adapters

import scala.concurrent.*
import shared.domain.EventSourcing.*
import shared.technologies.Kafka.Producer
import shared.ports.CommandSide
import upickle.default.*

abstract class CommandSideKafkaAdapter[TId, T <: Entity[
  TId
], Error, C <: Command[TId, T, Error]](
    bootstrapServers: String,
    clientId: String,
    topic: String
)(using ReadWriter[C])
    extends CommandSide[TId, T, Error, C]:

  lazy val producer = Producer(bootstrapServers, clientId)

  given ReadWriter[CommandId] = ReadWriter.derived

  def publish(command: C)(using ExecutionContext): Future[Unit] =
    Producer.send(producer, topic, command.id, command).map(_ => ())

package shared.adapters.cqrs

import scala.concurrent.*
import shared.domain.EventSourcing.*
import shared.technologies.Kafka.Producer
import shared.ports.cqrs.CommandSide
import upickle.default.*

abstract class CommandSideKafkaAdapter[TId, T <: Entity[
  TId
], Error, Env, C <: Command[TId, T, Error, Env, C]](
    bootstrapServers: String,
    clientId: String,
    topic: String
)(using ReadWriter[C])
    extends CommandSide[TId, T, Error, Env, C]:

  lazy val producer = Producer(bootstrapServers, clientId)

  given ReadWriter[CommandId] = ReadWriter.derived

  def publish(command: C)(using ExecutionContext): Future[Unit] =
    Producer.send(producer, topic, command.id, command).map(_ => ())

  import shared.technologies.Kafka
  import shared.technologies.Kafka.Reachable.*
  override def healthCheck(using ExecutionContext): Future[Option[String]] =
    for isReachable <- Kafka.isReachable(bootstrapServers)
    yield (isReachable match
      case Reachable        => None
      case Unreachable(err) => Some(err)
    )

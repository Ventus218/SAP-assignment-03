package ebikes.adapters.persistence

import scala.concurrent.*
import scala.jdk.CollectionConverters.*
import org.apache.kafka.common.TopicPartition
import shared.technologies.Kafka
import ebikes.domain.model.*
import ebikes.domain.model.EBikeEvent.*
import ebikes.ports.persistence.EBikesEventStore.*

class KafkaEBikesEventStoreAdapter(bootstrapServers: String)
    extends EBikesEventStore {
  private lazy val producer = Kafka.Producer(bootstrapServers)
  private val topic = "EBikeEvents"

  import upickle.default.*
  given ReadWriter[EBikeId] = ReadWriter.derived
  given ReadWriter[V2D] = ReadWriter.derived
  given ReadWriter[EBike] = ReadWriter.derived
  given ReadWriter[EBikeEvent] = ReadWriter.derived

  override def publish(
      e: EBikeEvent
  )(using ec: ExecutionContext): Future[Unit] =
    for
      _ <- createTopicIfNotExist()
      _ <- Kafka.Producer.send(producer, topic, e.key, e)
    yield ()

  override def allEvents()(using
      ec: ExecutionContext
  ): Future[Iterable[EBikeEvent]] =
    for
      _ <- createTopicIfNotExist()
      res <-
        Future:
          Kafka.Consumer.autocloseable(bootstrapServers): consumer =>
            consumer.subscribe(List(topic).asJava)
            consumer.seekToBeginning(List().asJava)
            Iterator
              .continually:
                val res = consumer.poll(java.time.Duration.ofMillis(20)).asScala
                consumer.commitSync()
                res
              .takeWhile(_.nonEmpty)
              .flatten
              .map(r => read[EBikeEvent](r.value()))
              .toSeq
    yield (res)

  // ***** This section is responsible for creating the topic if it doesn't exist *****
  import Kafka.Topics.*

  private var _topicCreated = false
  private def topicCreated: Boolean = synchronized(_topicCreated)
  private def topicCreated_=(newValue: Boolean): Unit =
    synchronized:
      _topicCreated = newValue

  private def createTopicIfNotExist()(using
      ec: ExecutionContext
  ): Future[Unit] =
    if topicCreated then Future.unit
    else
      TopicBuilder(bootstrapServers, topic).create
        .map(_ match
          case Left(_)  => topicCreated = true
          case Right(_) => ()
        )
}

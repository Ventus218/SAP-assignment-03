package ebikes.adapters.persistence

import scala.concurrent.*
import scala.jdk.CollectionConverters.*
import org.apache.kafka.common.TopicPartition
import shared.technologies.Kafka
import shared.adapters.persistence.EventSourcedEntity.*
import ebikes.domain.model.*
import ebikes.domain.model.EBikeEvent.*
import ebikes.ports.persistence.EBikesEventStore

class KafkaEBikesEventStoreAdapter(bootstrapServers: String)
    extends EBikesEventStore {
  private lazy val producer = Kafka.Producer(bootstrapServers, "EBikes")
  private val topic = "EBikeEvents"

  import upickle.default.*
  given ReadWriter[EBikeId] = ReadWriter.derived
  given ReadWriter[V2D] = ReadWriter.derived
  given ReadWriter[EBike] = ReadWriter.derived
  given ReadWriter[EBikeEvent] = ReadWriter.derived

  given EntityEvent[EBikeEvent, EBike, EBikeId] with
    extension (t: EBikeEvent)
      override def entityId: EBikeId = t match
        case Registered(eBike)                => eBike.id
        case UpdatedPhisicalData(id, _, _, _) => id

  given RootEvent[Registered, EBike, EBikeId] with
    extension (t: Registered) override def apply(): EBike = t.eBike

  given NonRootEvent[UpdatedPhisicalData, EBike, EBikeId] with
    extension (t: UpdatedPhisicalData)
      override def apply(e: EBike): EBike =
        e.copy(
          location = t.location.getOrElse(e.location),
          direction = t.direction.getOrElse(e.direction),
          speed = t.speed.getOrElse(e.speed)
        )

  def publish(event: EBikeEvent)(using ExecutionContext): Future[Unit] =
    for
      _ <- createTopicIfNotExist()
      _ <- Kafka.Producer.send(
        producer,
        topic,
        event.entityId,
        event
      )
    yield ()

  def allEvents()(using ExecutionContext): Future[Iterable[EBikeEvent]] =
    for
      _ <- createTopicIfNotExist()
      res <-
        Future:
          Kafka.Consumer.autocloseable(bootstrapServers): consumer =>
            consumer.assign(List(TopicPartition(topic, 0)).asJava)
            consumer.seekToBeginning(List().asJava)
            Iterator
              .continually:
                consumer.poll(java.time.Duration.ofMillis(20)).asScala
              .takeWhile(_.nonEmpty)
              .flatten
              .map(r => read[EBikeEvent](r.value()))
              .toSeq
    yield (res)

  def find(id: EBikeId)(using ExecutionContext): Future[Option[EBike]] =
    allEvents().map(_.toEntity(id))

  def all()(using ExecutionContext): Future[Iterable[EBike]] =
    allEvents().map(_.all())

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
      TopicBuilder(
        bootstrapServers,
        topic,
        partitions = Some(1),
        // don't worry, kafka is configured to retain log indefinetly
        cleanupPolicy = Some(Kafka.Topics.CleanupPolicy.Delete)
      ).create
        .map(_ match
          case Left(_)  => topicCreated = true
          case Right(_) => ()
        )
}

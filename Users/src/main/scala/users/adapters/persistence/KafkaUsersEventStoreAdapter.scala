package users.adapters.persistence

import scala.concurrent.*
import scala.jdk.CollectionConverters.*
import org.apache.kafka.common.TopicPartition
import shared.technologies.Kafka
import users.domain.model.*
import users.ports.persistence.UsersEventStore.*

class KafkaUsersEventStoreAdapter(bootstrapServers: String)
    extends UsersEventStore {
  private lazy val producer = Kafka.Producer(bootstrapServers)
  private val topic = "CreateUserEvent"

  import upickle.default.*
  given ReadWriter[Username] = ReadWriter.derived
  given ReadWriter[User] = ReadWriter.derived
  given ReadWriter[CreateUserEvent] = ReadWriter.derived

  override def publish(e: CreateUserEvent)(using
      ec: ExecutionContext
  ): Future[Unit] =
    for
      _ <- createTopicIfNotExist()
      _ <- Kafka.Producer.send(producer, topic, e.key, e)
    yield ()

  override def allEvents()(using
      ec: ExecutionContext
  ): Future[Iterable[CreateUserEvent]] =
    for
      _ <- createTopicIfNotExist()
      res <-
        Future:
          Kafka.Consumer.autocloseable(bootstrapServers): consumer =>
            consumer.assign(List(TopicPartition(topic, 0)).asJava)
            consumer.seekToBeginning(List().asJava)
            Iterator
              .continually(
                consumer.poll(java.time.Duration.ofMillis(20)).asScala
              )
              .takeWhile(_.nonEmpty)
              .flatten
              .map(r => read[CreateUserEvent](r.value()))
              .toSeq
    yield (res)

  // ***** This section is responsible for creating the topic if it doesn't exist *****
  import Kafka.Topics.*
  import CleanupPolicy.*

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
        cleanupPolicy = Some(Compact)
      ).create
        .map(_ match
          case Left(_)  => topicCreated = true
          case Right(_) => ()
        )
}

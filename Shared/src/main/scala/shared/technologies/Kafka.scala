package shared.technologies

import scala.concurrent.*
import scala.jdk.OptionConverters.*
import scala.jdk.CollectionConverters.*
import java.util.Properties
import upickle.default.*
import scala.util.Success
import scala.util.Failure

object Kafka:
  object Consumer:
    import org.apache.kafka.clients.consumer.KafkaConsumer

    /** Creates a kafka consumer with random unique group id and offset pointing
      * at the beginning of the queue
      */
    def apply(bootstrapServers: String): KafkaConsumer[String, String] =
      val config = new Properties();
      config.setProperty("bootstrap.servers", bootstrapServers);
      config.setProperty("group.id", java.util.UUID.randomUUID().toString())
      config.setProperty("auto.offset.reset", "earliest")
      config.setProperty(
        "key.deserializer",
        "org.apache.kafka.common.serialization.StringDeserializer"
      );
      config.setProperty(
        "value.deserializer",
        "org.apache.kafka.common.serialization.StringDeserializer"
      );
      KafkaConsumer[String, String](config);

    def autocloseable[A](bootstrapServers: String)(
        f: KafkaConsumer[String, String] => A
    ): A =
      val consumer = Consumer(bootstrapServers)
      try {
        f(consumer)
      } finally {
        consumer.close()
      }

  object Producer:
    import org.apache.kafka.clients.producer.*

    def apply(
        bootstrapServers: String,
        clientId: String
    ): KafkaProducer[String, String] =
      val config = Properties();
      config.setProperty("client.id", clientId);
      config.setProperty("bootstrap.servers", bootstrapServers);
      config.setProperty(
        "key.serializer",
        "org.apache.kafka.common.serialization.StringSerializer"
      );
      config.setProperty(
        "value.serializer",
        "org.apache.kafka.common.serialization.StringSerializer"
      );
      KafkaProducer[String, String](config)

    def send[K: ReadWriter, V: ReadWriter](
        producer: KafkaProducer[String, String],
        topic: String,
        key: K,
        value: V
    )(using ec: ExecutionContext): Future[RecordMetadata] =
      send(producer, topic, None, key, value)

    def send[K: ReadWriter, V: ReadWriter](
        producer: KafkaProducer[String, String],
        topic: String,
        partition: Option[Int],
        key: K,
        value: V
    )(using ec: ExecutionContext): Future[RecordMetadata] =
      val keyString = write(key)
      val valueString = write(value)
      val record = partition
        .map(ProducerRecord(topic, _, keyString, valueString))
        .getOrElse(ProducerRecord(topic, keyString, valueString))

      Future(producer.send(record).get())

  object Topics:
    import org.apache.kafka.clients.admin.*
    import org.apache.kafka.common.config.TopicConfig
    import org.apache.kafka.common.errors.TopicExistsException

    enum CleanupPolicy:
      case Compact
      case Delete

    case class TopicAlreadyExists()

    case class TopicBuilder(
        bootstrapServers: String,
        name: String,
        partitions: Option[Int] = None,
        replicationFactor: Option[Short] = None,
        cleanupPolicy: Option[CleanupPolicy] = None
    )(using ec: ExecutionContext):
      def create: Future[Either[TopicAlreadyExists, Unit]] =
        val config = new Properties();
        config.setProperty("bootstrap.servers", bootstrapServers)

        val admin = Admin.create(config)
        val topic = NewTopic(
          name,
          partitions.map(java.lang.Integer.valueOf(_)).toJava,
          replicationFactor.map(java.lang.Short.valueOf(_)).toJava
        )
        val configs = cleanupPolicy match
          case None => Map()
          case Some(CleanupPolicy.Compact) =>
            Map(
              (TopicConfig.CLEANUP_POLICY_CONFIG -> TopicConfig.CLEANUP_POLICY_COMPACT)
            )
          case Some(CleanupPolicy.Delete) =>
            Map(
              (TopicConfig.CLEANUP_POLICY_CONFIG -> TopicConfig.CLEANUP_POLICY_DELETE)
            )
        topic.configs(configs.asJava)

        Future:
          val createTopicOperation =
            admin.createTopics(List(topic).asJava).all()

          try Right(createTopicOperation.get())
          catch
            case e
                if createTopicOperation
                  .exceptionNow()
                  .isInstanceOf[TopicExistsException] =>
              Left(TopicAlreadyExists())

  enum Reachable:
    case Reachable
    case Unreachable(errMessage: String)

  def isReachable(
      bootstrapServers: String
  )(using ec: ExecutionContext): Future[Reachable] =
    Future {
      Consumer.autocloseable(bootstrapServers) { consumer =>
        consumer.listTopics(java.time.Duration.ofSeconds(5))
      }
    }
      .map(_ => Reachable.Reachable)
      .recover({ case exception =>
        Reachable.Unreachable(exception.getMessage())
      })

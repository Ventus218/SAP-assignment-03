package users

import scala.concurrent.*
import scala.jdk.OptionConverters.*
import scala.jdk.CollectionConverters.*
import java.util.Properties
import upickle.default.*
import org.apache.kafka.clients.producer.*
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.admin.*
import org.apache.kafka.common.config.TopicConfig
import org.apache.kafka.common.errors.TopicExistsException

object Kafka {

  private val producerConfig = Properties();
  producerConfig.setProperty("client.id", "Users");
  producerConfig.setProperty(
    "bootstrap.servers",
    "users-es:9092"
  ); // TODO: externalize config
  producerConfig.setProperty(
    "key.serializer",
    "org.apache.kafka.common.serialization.StringSerializer"
  );
  producerConfig.setProperty(
    "value.serializer",
    "org.apache.kafka.common.serialization.StringSerializer"
  );

  val producer = KafkaProducer[String, String](producerConfig);

  def newConsumer: KafkaConsumer[String, String] =
    val consumerConfig = new Properties();
    consumerConfig.setProperty(
      "bootstrap.servers",
      "users-es:9092"
    ); // TODO: externalize config
    consumerConfig.setProperty(
      "key.deserializer",
      "org.apache.kafka.common.serialization.StringDeserializer"
    );
    consumerConfig.setProperty(
      "value.deserializer",
      "org.apache.kafka.common.serialization.StringDeserializer"
    );
    KafkaConsumer[String, String](consumerConfig);

  def withNewConsumer[A](f: KafkaConsumer[String, String] => A): A =
    val consumer = newConsumer
    try {
      f(consumer)
    } finally {
      consumer.close()
    }

  def send(
      topic: String,
      partition: Option[Int],
      key: String,
      value: String
  )(using ec: ExecutionContext): Future[RecordMetadata] =
    val record = partition
      .map(ProducerRecord(topic, _, key, value))
      .getOrElse(ProducerRecord(topic, key, value))

    Future(producer.send(record).get())

  // ***** SOME USEFUL send OVERLOADS *****
  def send(
      topic: String,
      key: String,
      value: String
  )(using ec: ExecutionContext): Future[RecordMetadata] =
    send(topic, None, key, value)

  def send[T: ReadWriter](
      topic: String,
      key: String,
      value: T
  )(using ec: ExecutionContext): Future[RecordMetadata] =
    send(topic, key, write(value))

  def send[T: ReadWriter](
      topic: String,
      partition: Option[Int],
      key: String,
      value: T
  )(using ec: ExecutionContext): Future[RecordMetadata] =
    send(topic, partition, key, write(value))

  enum CleanupPolicy:
    case Compact
    case Delete

  case class TopicAlreadyExists()

  case class TopicBuilder(
      name: String,
      partitions: Option[Int] = None,
      replicationFactor: Option[Short] = None,
      cleanupPolicy: Option[CleanupPolicy] = None
  )(using ec: ExecutionContext):
    def create: Future[Either[TopicAlreadyExists, Unit]] =
      val config = new Properties();
      config.setProperty(
        "bootstrap.servers",
        "users-es:9092"
      ); // TODO: externalize config

      val admin = Admin.create(config)
      val topic = NewTopic(
        name,
        partitions.map(java.lang.Integer(_)).toJava,
        replicationFactor.map(java.lang.Short(_)).toJava
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
        val createTopicOperation = admin.createTopics(List(topic).asJava).all()

        try Right(createTopicOperation.get())
        catch
          case e
              if createTopicOperation
                .exceptionNow()
                .isInstanceOf[TopicExistsException] =>
            Left(TopicAlreadyExists())

}

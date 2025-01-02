package users

import scala.concurrent.*
import java.util.Properties
import upickle.default.*
import org.apache.kafka.clients.producer.*

object Kafka {

  private val config = Properties();
  config.put("client.id", "Users");
  config.put("bootstrap.servers", "users-es:9092"); // TODO: externalize config
  config.put(
    "key.serializer",
    "org.apache.kafka.common.serialization.StringSerializer"
  );
  config.put(
    "value.serializer",
    "org.apache.kafka.common.serialization.StringSerializer"
  );

  val producer = KafkaProducer[String, String](config);

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

}

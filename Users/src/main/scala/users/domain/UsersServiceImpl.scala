package users.domain;

import scala.concurrent.*
import users.domain.model.*;
import users.domain.errors.*
import users.ports.persistence.UsersRepository;

// TODO: remove
import upickle.default.*
import users.Kafka
import scala.collection.JavaConverters.*
import users.Kafka.CleanupPolicy
import org.apache.kafka.common.TopicPartition

class UsersServiceImpl(private val usersRepository: UsersRepository)
    extends UsersService:
  val topic = "users"

  // TODO: remove
  given ReadWriter[Username] = ReadWriter.derived
  given ReadWriter[User] = ReadWriter.derived

  override def registerUser(
      username: Username
  )(using ec: ExecutionContext): Future[Either[UsernameAlreadyInUse, User]] =
    // This way when the registerUser future completes it is sure that calls to users() will contain the new user
    def waitForUserCreation(username: Username): Future[Unit] =
      for
        users <- users()
        _ <-
          if users.exists(_.username == username) then Future.unit
          else waitForUserCreation(username)
      yield ()

    val user = User(username)
    for
      users <- users() // creates the topic if not exist
      res <-
        if users.exists(_ == user) then
          Future(Left(UsernameAlreadyInUse(username)))
        else
          Kafka.send("users", username.value, user)
          waitForUserCreation(username).map(_ => Right(user))
    yield (res)

  override def users()(using ec: ExecutionContext): Future[Iterable[User]] =
    for
      _ <- createTopicIfNotExist()
      res <-
        Future:
          Kafka.withNewConsumer: consumer =>
            consumer.assign(List(TopicPartition(topic, 0)).asJava)
            consumer.seekToBeginning(List().asJava)
            Iterator
              .continually(
                consumer.poll(java.time.Duration.ofMillis(20)).asScala
              )
              .takeWhile(_.nonEmpty)
              .flatten
              .map(r => (Username(r.key()), read[User](r.value())))
              .toMap
              .values
    yield (res)

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
      Kafka
        .TopicBuilder(
          "users",
          partitions = Some(1),
          cleanupPolicy = Some(CleanupPolicy.Compact)
        )
        .create
        .map(_ match
          case Left(_)  => topicCreated = true
          case Right(_) => ()
        )

  override def healthCheckError(): Option[String] = None

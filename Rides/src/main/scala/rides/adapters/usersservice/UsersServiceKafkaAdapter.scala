package rides.adapters.usersservice

import scala.concurrent.*
import scala.jdk.CollectionConverters.*
import upickle.default.*
import shared.technologies.Kafka.Consumer
import rides.domain.model.*
import rides.ports.UsersService

class UsersServiceKafkaAdapter(bootstrapServers: String, topic: String)
    extends UsersService:

  import shared.domain.EventSourcing.CommandId
  given ReadWriter[CommandId] = ReadWriter.derived
  given ReadWriter[Username] = ReadWriter.derived
  // This message definition is trimmed to the bare minimum
  private case class Registered(id: CommandId, entityId: Username)
      derives ReadWriter

  private var _existingUsers = Set[Username]()
  private def existingUsers = synchronized(_existingUsers)
  private def existingUsers_=(v: Set[Username]) = synchronized {
    _existingUsers = v
  }

  Thread.ofVirtual
    .name("users-service-consumer")
    .start(() => {
      Consumer.autocloseable(bootstrapServers): consumer =>
        consumer.subscribe(List(topic).asJava)
        while true do
          val newUsers = Iterator
            .continually(
              consumer.poll(java.time.Duration.ofMillis(20)).asScala
            )
            .takeWhile(_.nonEmpty)
            .flatten
            .map(r => read[Registered](r.value()))
            .map(_.entityId)
            .toSeq
          if !newUsers.isEmpty then existingUsers = existingUsers ++ newUsers
    })

  override def exists(username: Username): Boolean =
    existingUsers(username)

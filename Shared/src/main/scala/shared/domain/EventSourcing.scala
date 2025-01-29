package shared.domain

object EventSourcing:
  trait Entity[Id]:
    def id: Id

  case class CommandId(value: String)
  object CommandId:
    def random(): CommandId =
      import java.util.UUID
      CommandId(UUID.randomUUID().toString())

  trait Command[TId, T <: Entity[TId], Error, Env, Self <: Command[
    TId,
    T,
    Error,
    Env,
    Self
  ]]:
    val id: CommandId
    val entityId: TId

    /** The timestamp at which the command was actually stored
      */
    val timestamp: Option[Long]

    def setTimestamp(timestamp: Long): Self

    /** Applies the command to the current state of entities
      * @return
      *   Either an error or the new set of entities
      */
    def apply(entities: Map[TId, T])(using Env): Either[Error, Map[TId, T]]

  extension [TId, T <: Entity[TId], Env](
      it: Iterable[Command[TId, T, ?, Env, ?]]
  )
    /** Applies in sequence all the commands which do not result in errors.
      *
      * @return
      *   The new set of entities
      */
    def applyCommands()(using Env): Map[TId, T] =
      it.foldLeft(Map[TId, T]())((entities, command) =>
        command(entities).getOrElse(entities)
      )

package shared.domain

object EventSourcing:
  trait Entity[Id]:
    def id: Id

  case class CommandId(value: String)
  object CommandId:
    def random(): CommandId =
      import java.util.UUID
      CommandId(UUID.randomUUID().toString())

  trait Command[TId, T <: Entity[TId], Error]:
    val id: CommandId
    val entityId: TId

    type CommandResult[E2 <: Error] = Either[E2, Option[T]]

    /** Applies the command to an optional previous version of an entity.
      *
      * If you plan on using this method directly you should check that the
      * entity id and the command's entityId are matching
      *
      * @param previous
      * @return
      *   The result of applying the command
      */
    def apply(previous: Option[T]): CommandResult[Error]

  extension [TId, T <: Entity[TId]](it: Iterable[Command[TId, T, ?]])
    /** Applies in sequence all the commands which do not result in errors.
      *
      * All the commands' entityIds should match, if not the first one is taken
      * as reference and others non maching are ignored.
      *
      * @return
      *   The event sourced entity or None if the entity is not created by any
      *   command
      */
    def applyCommands(): Option[T] =
      it.headOption match
        case None => None
        case Some(head) =>
          it.tail
            .filter(_.entityId == head.entityId)
            .foldLeft(head(None).toOption.flatten)((e, command) =>
              command(e).getOrElse(e)
            )

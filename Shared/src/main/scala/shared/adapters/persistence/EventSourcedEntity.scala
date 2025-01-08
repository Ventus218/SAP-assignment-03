package shared.adapters.persistence

object EventSourcedEntity:
  trait EntityEvent[T, E, ID]:
    extension (t: T) def entityId: ID

  trait RootEvent[T, E, ID]:
    extension (t: T) def apply(): E
  trait NonRootEvent[T, E, ID]:
    extension (t: T) def apply(e: E): E

  import scala.reflect.TypeTest
  extension [T, E, ID, RE, NRE](it: Iterable[T])
    def toEntity(id: ID)(using
        EntityEvent[T, E, ID],
        RootEvent[RE, E, ID],
        NonRootEvent[NRE, E, ID],
        TypeTest[T, RE],
        TypeTest[T, NRE]
    ): Option[E] =
      it.filter(_.entityId == id)
        .foldLeft(Option.empty[E])((entity, event) =>
          event match
            case re: RE   => Some(entity.getOrElse(re()))
            case nre: NRE => entity.map(nre(_))
            case _        => entity
        )

    def all()(using
        EntityEvent[T, E, ID],
        RootEvent[RE, E, ID],
        NonRootEvent[NRE, E, ID],
        TypeTest[T, RE],
        TypeTest[T, NRE]
    ): Iterable[E] =
      it.foldLeft(Map[ID, E]())((map, event) =>
        event match
          case re: RE   => map + (event.entityId -> re())
          case nre: NRE => map.updatedWith(event.entityId)(_.map(nre(_)))
          case _        => map
      ).values

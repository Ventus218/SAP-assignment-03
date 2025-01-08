package ebikes.ports.persistence

import scala.concurrent.*
import ebikes.domain.model.*
import shared.ports.persistence.EntityEventStore

trait EBikesEventStore extends EntityEventStore[EBikeEvent, EBike, EBikeId]

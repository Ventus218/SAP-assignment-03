package ebikes.ports.persistence

import scala.concurrent.*
import ebikes.domain.model.*

object EBikesEventStore:

  import EBikeEvent.*
  extension (e: EBikeEvent)
    def key: EBikeId = e match
      case Registered(eBike)                => eBike.id
      case UpdatedPhisicalData(id, _, _, _) => id

  trait EBikesEventStore:
    def publish(e: EBikeEvent)(using ec: ExecutionContext): Future[Unit]
    def allEvents()(using ec: ExecutionContext): Future[Iterable[EBikeEvent]]

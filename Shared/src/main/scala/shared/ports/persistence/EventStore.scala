package shared.ports.persistence

import scala.concurrent.*

trait EventStore[T]:
  def publish(event: T)(using ExecutionContext): Future[Unit]

  def allEvents()(using ExecutionContext): Future[Iterable[T]]

trait EntityEventStore[T, E, ID] extends EventStore[T]:
  def find(id: ID)(using ExecutionContext): Future[Option[E]]
  def all()(using ExecutionContext): Future[Iterable[E]]

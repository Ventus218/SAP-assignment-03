package shared.ports

import scala.concurrent.Future

trait MetricsService:
  def incrementCounter(counterId: String, amount: Long): Future[Unit]
  def incrementCounterByOne(counterId: String): Future[Unit] =
    incrementCounter(counterId, 1)
  def registerForHealthcheckMonitoring(selfAddress: String): Future[Unit]

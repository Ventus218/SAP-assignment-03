package shared.adapters

import scala.concurrent.*
import scala.concurrent.duration.Duration
import sttp.client4.*
import shared.ports.MetricsService
import upickle.default._

class MetricsServiceAdapter(private val address: String) extends MetricsService:

  private val rescheduleDelayMillis: Long = 5000

  private case class RegisterDTO(value: String) derives ReadWriter
  private case class IncrementCounterDTO(value: Long) derives ReadWriter

  lazy given executionContext: ExecutionContext = ExecutionContext.fromExecutor(
    java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()
  )

  def incrementCounter(counterId: String, amount: Long): Future[Unit] =
    for
      res <- quickRequest
        .post(uri"http://$address/metrics/counters/$counterId")
        .body(write(IncrementCounterDTO(amount)))
        .contentType("application/json")
        .send(DefaultFutureBackend())
    yield()

  def registerForHealthcheckMonitoring(selfAddress: String): Future[Unit] =
    (for
      res <- quickRequest
        .post(uri"http://$address/metrics/endpoints")
        .body(write(RegisterDTO(selfAddress)))
        .contentType("application/json")
        .readTimeout(Duration(5, "s"))
        .send(DefaultFutureBackend())
      _ <-
        if res.isSuccess then Future.unit
        else rescheduleAfterMillis(rescheduleDelayMillis, selfAddress)
    yield ())
    .recoverWith({
      case _: Throwable => rescheduleAfterMillis(rescheduleDelayMillis, selfAddress)
    })

  private def rescheduleAfterMillis(delay: Long, selfAddress: String): Future[Unit] =
    for
      _ <- Future(Thread.sleep(delay))
      _ <- registerForHealthcheckMonitoring(selfAddress)
    yield ()

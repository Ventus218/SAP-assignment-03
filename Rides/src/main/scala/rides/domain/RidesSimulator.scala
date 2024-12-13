package rides.domain

import java.util.concurrent.TimeUnit
import scala.util.Random
import scala.concurrent.*
import scala.concurrent.duration.*
import akka.actor.typed.ActorSystem
import rides.domain.model.*
import rides.ports.*
import rides.ports.EBikesService.UpdateEBikePhisicalDataDTO

/** This class is just a simulator of bikes actually moving while riding. Also
  * it blocks for each ebike position update which in a distributed system could
  * is really really bad.
  *
  * @param ridesService
  * @param simulationPeriod
  * @param as
  */
class RidesSimulator(
    private val ridesService: RidesService,
    private val eBikesService: EBikesService,
    private val simulationPeriod: FiniteDuration
)(using as: ActorSystem[?]):

  private given ExecutionContext = as.executionContext
  private val timeout = Duration.Inf
  private def simulationPeriodSeconds =
    simulationPeriod.toUnit(TimeUnit.SECONDS)

  def start(): Unit =
    as.scheduler.scheduleWithFixedDelay(simulationPeriod, simulationPeriod): () =>
      ridesService.transaction:
        ridesService
          .activeRides()
          .foreach(ride =>
            val eBikeId = ride.eBikeId
            sync(eBikesService.find(eBikeId))
              .foreach(eBike =>
                val newLocation =
                  eBike.location + (eBike.direction * simulationPeriodSeconds * eBike.speed)

                val shouldChangeDirection = Random.between(0, 5) == 0
                val newDir = Option.when(shouldChangeDirection)(
                  V2D(Random.between(0d, 1d), Random.between(0d, 1d))
                )

                val newSpeed = Option
                  .when(shouldChangeDirection)(0d)
                  .orElse(Some(eBike.speed))
                  .map(s => Math.min(s + simulationPeriodSeconds * 1, 5))

                sync(
                  eBikesService.updatePhisicalData(
                    eBikeId,
                    UpdateEBikePhisicalDataDTO(
                      Some(newLocation),
                      newDir,
                      newSpeed
                    )
                  )
                )
              )
          )

  private def sync[T](future: Future[T]): T =
    Await.result(future, timeout)

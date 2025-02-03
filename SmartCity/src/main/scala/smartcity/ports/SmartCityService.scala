package smartcity.ports

import smartcity.domain.model.*

object SmartCityService:
  final case class SemaphoreNotFound(id: SemaphoreId)
  final case class JunctionNotFound(id: JunctionId)

trait SmartCityService:
  import SmartCityService.*

  def junctions(): Iterable[Junction]

  def bestPath(
      from: JunctionId,
      to: JunctionId
  ): Either[JunctionNotFound, Seq[Street]]

  def semaphore(id: SemaphoreId): Either[SemaphoreNotFound, Semaphore]

  def healthCheckError(): Option[String]

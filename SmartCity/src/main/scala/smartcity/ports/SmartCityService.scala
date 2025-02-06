package smartcity.ports

import smartcity.domain.model.*

object SmartCityService:
  final case class SemaphoreNotFound(junctionId: JunctionId)
  final case class JunctionNotFound(id: JunctionId)

trait SmartCityService:
  import SmartCityService.*

  def junctions(): Iterable[Junction]

  def bestPath(
      from: JunctionId,
      to: JunctionId
  ): Either[JunctionNotFound, Seq[Street]]

  def semaphore(junctionId: JunctionId): Either[SemaphoreNotFound, Semaphore]

  def healthCheckError(): Option[String]

package smartcity.domain

import smartcity.ports.SmartCityService
import smartcity.domain.model.*
import scala.util.Random

class SmartCityServiceImpl extends SmartCityService:
  import SmartCityService.*

  private val random: Random = Random(1234)
  private val streets: Seq[Street] = Seq.tabulate(6): n =>
    Street(StreetId(s"S${n + 1}"), random.between(500, 3000))
  private def s(n: Int) = streets(n - 1)

  private val _junctions: Set[Junction] =
    val junctions = Seq.tabulate(5)(n => JunctionId(s"J${n + 1}"))

    def j(n: Int) = junctions(n - 1)

    Set(
      Junction(j(1), true, None, Set(s(1), s(2), s(3), s(4))),
      Junction(j(2), false, None, Set(s(1), s(5))),
      Junction(j(3), false, None, Set(s(2), s(5), s(6))),
      Junction(j(4), false, None, Set(s(3), s(6))),
      Junction(j(5), false, None, Set(s(4)))
    )

  override def junctions(): Iterable[Junction] =
    _junctions

  override def bestPath(
      from: JunctionId,
      to: JunctionId
  ): Either[JunctionNotFound, Seq[Street]] =
    (junctions().find(_.id == from), junctions().find(_.id == to)) match
      case (None, _) => Left(JunctionNotFound(from))
      case (_, None) => Left(JunctionNotFound(to))
      case (Some(from), Some(to)) =>
        Right(
          // I'm shameless
          (from.id.value, to.id.value) match
            case ("J1", "J1") => Seq()
            case ("J2", "J2") => Seq()
            case ("J3", "J3") => Seq()
            case ("J4", "J4") => Seq()
            case ("J5", "J5") => Seq()

            case ("J1", "J2") | ("J2", "J1") => Seq(s(1))
            case ("J1", "J3") | ("J3", "J1") => Seq(s(2))
            case ("J1", "J4") | ("J4", "J1") => Seq(s(2), s(5))
            case ("J1", "J5") | ("J5", "J1") => Seq(s(3))

            case ("J3", "J2") | ("J2", "J3") => Seq(s(4))
            case ("J3", "J4") | ("J4", "J3") => Seq(s(5))
            case ("J3", "J5") | ("J5", "J3") => Seq(s(5), s(6))

            case ("J4", "J2") | ("J2", "J4") => Seq(s(5), s(4))
            case ("J4", "J5") | ("J5", "J4") => Seq(s(6))

            case ("J5", "J2") | ("J2", "J5") => Seq(s(3), s(1))
        )

  override def semaphore(
      id: SemaphoreId
  ): Either[SemaphoreNotFound, Semaphore] =
    junctions()
      .map(_.semaphore)
      .flatten
      .find(_.id == id)
      .toRight(SemaphoreNotFound(id))

  override def healthCheckError(): Option[String] = None

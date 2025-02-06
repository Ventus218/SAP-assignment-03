package ebikes

import scala.util.Random
import sttp.client4.*
import sttp.model.*
import upickle.default.*
import ebikes.domain.model.*

class ABikesSimulator(
    ridesServiceAddress: String,
    smartCityServiceAddress: String
) extends Runnable:
  import Utils.*
  import ABikesSimulator.*

  private var _activeRides: Map[RideId, Ride] = Map()
  private def activeRides: Map[RideId, Ride] = synchronized(_activeRides)
  private def setActiveRides(f: Map[RideId, Ride] => Map[RideId, Ride]) =
    synchronized(this._activeRides = f(this._activeRides))

  private var junctions: Map[JunctionId, Junction] = Map()
  private def chargingStation: Junction =
    junctions.values.find(_.hasChargingStation).get

  private val ridesUri = s"http://$ridesServiceAddress/rides"
  private val smartCityUri = s"http://$smartCityServiceAddress/smartcity"

  def getStreetsGraph(): Unit =
    val body = requestOrLogError(
      uri"$smartCityUri/junctions",
      retryUntilSuccessInterval = 1000
    ).get
    junctions = read[Seq[Junction]](body).map(j => (j.id -> j)).toMap

  def run(): Unit =
    getStreetsGraph()
    while true do
      requestOrLogError(uri"$ridesUri/active").foreach: ridesBody =>
        val rides = read[Set[Ride]](ridesBody)
        val newRides = rides.map(_.id) -- activeRides.keySet
        setActiveRides(_ ++ rides.map(r => (r.id -> r)))
        newRides.foreach: id =>
          Thread.ofVirtual().start(() => rideSimulator(id))
      Thread.sleep(1000)

  private def rideSimulator(id: RideId): Unit =
    var waitForStateChange = false
    var currentJunction = chargingStation

    def ridePath(
        from: JunctionId,
        to: JunctionId
    )(using logPrefix: String = ""): Unit =
      val pathBody = requestOrLogError(
        uri"$smartCityUri/path?from=${from.value}&to=${to.value}",
        retryUntilSuccessInterval = 1000
      ).get
      val path = read[Seq[Street]](pathBody)
      path.foreach: street =>
        currentJunction.semaphore match
          case None => ()
          case Some(Semaphore(SemaphoreId(id), _, _, _, _)) =>
            val semaphoreBody = requestOrLogError(
              uri"$smartCityUri/semaphores/$id",
              retryUntilSuccessInterval = 1000
            ).get
            val sem = read[Semaphore](semaphoreBody)
            print(s"${logPrefix}Semaphore $id is ${sem.state}")
            if (sem.state == SemaphoreState.Red)
              print(s", wait for ${sem.nextChangeStateTimestamp}ms")
              Thread.sleep(sem.nextChangeStateTimestamp)
            println("")
        println(s"${logPrefix}Going through street ${street.id.value}")
        Thread.sleep(street.timeLengthMillis)

    while true do
      val ride = activeRides(id)
      val bikeId = ride.eBikeId.value
      given String = s"bikeId: "
      ride.status match
        case RideStatus.BikeGoingToUser(junctionId) =>
          if waitForStateChange then ()
          else
            ridePath(chargingStation.id, junctionId)
            println(s"$bikeId: Arrived to user!")
            requestOrLogError(
              uri"$ridesUri/${id.value}/eBikeArrivedToUser",
              Method.PUT,
              retryUntilSuccessInterval = 500
            )
            waitForStateChange = true
        case RideStatus.UserRiding =>
          // simulate random riding
          // TODO: maybe can reuse ridePath
          val streetChoice = Random.shuffle(currentJunction.streets.toSeq).head
          val streetId = streetChoice.id.value
          val streetLength = streetChoice.timeLengthMillis
          println(s"$bikeId: Taking street $streetId for ${streetLength}ms")
          Thread.sleep(streetLength)
          currentJunction = (junctions.values.toSet - currentJunction)
            .find(_.streets(streetChoice))
            .get
          println(s"$bikeId: Arrived at junction ${currentJunction.id.value}")
        case RideStatus.BikeGoingBackToStation =>
          ridePath(currentJunction.id, chargingStation.id)
          requestOrLogError(
            uri"$ridesUri/${id.value}/eBikeReachedStation",
            Method.PUT,
            retryUntilSuccessInterval = 500
          )
          setActiveRides(_ - id)
          return // exits function (terminating the thread)
      Thread.sleep(100)

  def requestOrLogError(
      uri: Uri,
      method: Method = Method.GET,
      retryUntilSuccessInterval: Long = 0
  )(using logPrefix: String = ""): Option[String] =
    var result = Option.empty[String]
    while
      result = quickRequest.method(method, uri).send(DefaultSyncBackend()) match
        case res if res.isSuccess => Some(res.body)
        case res =>
          println(
            s"${logPrefix}${method.method} request $uri failed with status ${res.code}: ${res.body}"
          )
          if (retryUntilSuccessInterval != 0)
            println(s"${logPrefix}retrying in ${retryUntilSuccessInterval}ms")
            Thread.sleep(retryUntilSuccessInterval)
          else ()
          None
      result.isEmpty && retryUntilSuccessInterval != 0
    do ()
    result

private object Utils:
  import scala.concurrent.*
  import sttp.model.MediaType

  extension [T](r: Request[T])
    def jsonBody(body: String): Request[T] =
      r.body(body).contentType(MediaType.ApplicationJson)

    def jsonBody[U: ReadWriter](body: U): Request[T] =
      r.body(write(body)).contentType(MediaType.ApplicationJson)

    def sendAsync()(using
        ExecutionContext
    ): Future[Either[String, Response[T]]] =
      r.send(DefaultFutureBackend())
        .map(Right(_))
        .recover({ case t: Throwable =>
          Left(t.getMessage())
        })

object ABikesSimulator:
  private case class RideId(value: String) derives ReadWriter

  private case class Ride(
      id: RideId,
      eBikeId: EBikeId,
      status: RideStatus
  ) derives ReadWriter

  enum RideStatus derives ReadWriter:
    case BikeGoingToUser(junctionId: JunctionId)
    case UserRiding
    case BikeGoingBackToStation

  given ReadWriter[EBikeId] = ReadWriter.derived
  import java.util.Date
  given ReadWriter[Date] =
    readwriter[Long].bimap(
      date => date.getTime(),
      long => Date(long)
    )

  case class SemaphoreId(value: String) derives ReadWriter
  enum SemaphoreState derives ReadWriter:
    case Red
    case Green
  case class Semaphore(
      id: SemaphoreId,
      state: SemaphoreState,
      timeGreenMillis: Long,
      timeRedMillis: Long,
      nextChangeStateTimestamp: Long
  ) derives ReadWriter

  case class StreetId(value: String) derives ReadWriter
  case class Street(id: StreetId, timeLengthMillis: Long) derives ReadWriter

  case class JunctionId(value: String) derives ReadWriter
  case class Junction(
      id: JunctionId,
      hasChargingStation: Boolean,
      semaphore: Option[Semaphore] = None,
      streets: Set[Street]
  ) derives ReadWriter

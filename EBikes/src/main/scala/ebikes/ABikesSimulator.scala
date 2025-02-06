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
    val body = quickRequest
      .get(uri"$smartCityUri/junctions")
      .sendSyncLogError(retryUntilSuccessInterval = 1000)
      .get
    junctions = read[Seq[Junction]](body).map(j => (j.id -> j)).toMap

  def run(): Unit =
    getStreetsGraph()
    while true do
      quickRequest
        .get(uri"$ridesUri/active")
        .sendSyncLogError()
        .foreach: ridesBody =>
          val rides = read[Set[Ride]](ridesBody)
          val newRides = rides.map(_.id) -- activeRides.keySet
          setActiveRides(_ ++ rides.map(r => (r.id -> r)))
          newRides.foreach: id =>
            Thread.ofVirtual().start(() => rideSimulator(id))
      Thread.sleep(1000)

  private def rideSimulator(id: RideId): Unit =
    var waitForStateChange = false
    var currentJunction = chargingStation

    def ridePath(path: Seq[Street])(using logPrefix: String = ""): Unit =
      path.foreach: street =>
        currentJunction.semaphore match
          case None => ()
          case Some(Semaphore(JunctionId(id), _, _, _, _)) =>
            val semaphoreBody = quickRequest
              .get(uri"$smartCityUri/semaphores/$id")
              .sendSyncLogError(retryUntilSuccessInterval = 1000)
              .get
            val sem = read[Semaphore](semaphoreBody)
            print(s"${logPrefix}Semaphore on junction $id is ${sem.state}")
            if (sem.state == SemaphoreState.Red)
              val waitTime =
                sem.nextChangeStateTimestamp - System.currentTimeMillis()
              print(s", wait for ${waitTime}ms")
              if waitTime > 0 then Thread.sleep(waitTime)
            println("")
        val streetId = street.id.value
        val streetLength = street.timeLengthMillis
        println(s"${logPrefix}Riding street $streetId for ${streetLength}ms")
        Thread.sleep(streetLength)
        currentJunction = junctions(
          (junctions.keySet - currentJunction.id)
            .find(k => junctions(k).streets(street))
            .get
        )
        println(s"${logPrefix}Arrived at junction ${currentJunction.id.value}")

    def getBestPath(from: JunctionId, to: JunctionId): Seq[Street] =
      val pathBody = quickRequest
        .get(uri"$smartCityUri/path?from=${from.value}&to=${to.value}")
        .sendSyncLogError(retryUntilSuccessInterval = 1000)
        .get
      read[Seq[Street]](pathBody)

    while true do
      val ride = activeRides(id)
      val bikeId = ride.eBikeId.value
      given String = s"$bikeId: "
      ride.status match
        case RideStatus.BikeGoingToUser(junctionId) =>
          if waitForStateChange then ()
          else
            println(s"$bikeId: Ride requested, going to ${ride.username.value}")
            ridePath(getBestPath(chargingStation.id, junctionId))
            println(s"$bikeId: Arrived to ${ride.username.value}!")
            quickRequest
              .put(uri"$ridesUri/${id.value}/eBikeArrivedToUser")
              .sendSyncLogError(retryUntilSuccessInterval = 500)
            waitForStateChange = true
        case RideStatus.UserRiding =>
          // simulate random riding
          ridePath(Random.shuffle(currentJunction.streets.toSeq).take(1))
        case RideStatus.BikeGoingBackToStation =>
          println(s"$bikeId: Ride finished, going back to charging station!")
          ridePath(getBestPath(currentJunction.id, chargingStation.id))
          quickRequest
            .put(uri"$ridesUri/${id.value}/eBikeReachedStation")
            .sendSyncLogError(retryUntilSuccessInterval = 500)
          setActiveRides(_ - id)
          println(s"$bikeId: Arrived back to charging station!")
          return // exits function (terminating the thread)
      Thread.sleep(100)

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

  extension (r: Request[String])
    def sendSyncLogError(retryUntilSuccessInterval: Long = 0)(using
        logPrefix: String = ""
    ): Option[String] =
      var result = Option.empty[String]
      while
        result = r.send(DefaultSyncBackend()) match
          case res if res.isSuccess => Some(res.body)
          case res =>
            println(
              s"${logPrefix}${r.method} request ${r.uri} failed with status ${res.code}: ${res.body}"
            )
            if (retryUntilSuccessInterval != 0)
              println(
                s"${logPrefix}retrying in ${retryUntilSuccessInterval}ms"
              )
              Thread.sleep(retryUntilSuccessInterval)
            else ()
            None
        result.isEmpty && retryUntilSuccessInterval != 0
      do ()
      result

object ABikesSimulator:
  private case class Username(value: String) derives ReadWriter
  private case class RideId(value: String) derives ReadWriter

  private case class Ride(
      id: RideId,
      username: Username,
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

  enum SemaphoreState derives ReadWriter:
    case Red
    case Green
  case class Semaphore(
      junctionId: JunctionId,
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

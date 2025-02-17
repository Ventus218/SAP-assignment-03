package abikessimulator

import scala.util.*
import sttp.client4.*
import sttp.model.*
import upickle.default.*

class ABikesSimulator(
    eBikesServiceAddress: String,
    ridesServiceAddress: String,
    smartCityServiceAddress: String
):
  import Utils.*
  import ABikesSimulator.*

  private var _activeRides: Map[RideId, Ride] = Map()
  private def activeRides: Map[RideId, Ride] = synchronized(_activeRides)
  private def setActiveRides(f: Map[RideId, Ride] => Map[RideId, Ride]) =
    synchronized(this._activeRides = f(this._activeRides))

  private var junctions: Map[JunctionId, Junction] = Map()
  private def chargingStation: Junction =
    junctions.values.find(_.hasChargingStation).get

  private val eBikesUri = s"http://$eBikesServiceAddress/ebikes"
  private val ridesUri = s"http://$ridesServiceAddress/rides"
  private val smartCityUri = s"http://$smartCityServiceAddress/smartcity"

  def getStreetsGraph(): Unit =
    val body = quickRequest
      .get(uri"$smartCityUri/junctions")
      .sendSyncLogError(retryUntilSuccessInterval = 1000)
      .get
    junctions = read[Seq[Junction]](body).map(j => (j.id -> j)).toMap

  def execute(): Unit =
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
    var eBikeId = activeRides(id).eBikeId

    def ridePath(path: Seq[Street])(using logPrefix: String = ""): Unit =
      import EBikeLocation.*
      path.foreach: street =>
        updateEBikeLocation(eBikeId, Junction(currentJunction.id))
        currentJunction.semaphore match
          case None => ()
          case Some(Semaphore(JunctionId(id), _, _, _, _)) =>
            val semaphoreBody = quickRequest
              .get(uri"$smartCityUri/semaphores/$id")
              .sendSyncLogError(retryUntilSuccessInterval = 1000)
              .get
            val sem = read[Semaphore](semaphoreBody)
            if sem.state == SemaphoreState.Red then
              val waitTime =
                sem.nextChangeStateTimestamp - System.currentTimeMillis()
              if waitTime > 0 then Thread.sleep(waitTime)
        val streetId = street.id.value
        val streetLength = street.timeLengthMillis
        updateEBikeLocation(eBikeId, Street(street.id))
        Thread.sleep(streetLength)
        currentJunction = junctions(
          (junctions.keySet - currentJunction.id)
            .find(k => junctions(k).streets(street))
            .get
        )
        updateEBikeLocation(eBikeId, Junction(currentJunction.id))

    def getBestPath(from: JunctionId, to: JunctionId): Seq[Street] =
      val pathBody = quickRequest
        .get(uri"$smartCityUri/path?from=${from.value}&to=${to.value}")
        .sendSyncLogError(retryUntilSuccessInterval = 1000)
        .get
      read[Seq[Street]](pathBody)

    def updateEBikeLocation(eBikeId: EBikeId, location: EBikeLocation): Unit =
      quickRequest
        .patch(uri"$eBikesUri/${eBikeId.value}")
        .jsonBody(UpdateEBikeLocationDTO(location))
        .sendSyncLogError()

    while true do
      val ride = activeRides(id)
      val bikeId = ride.eBikeId.value
      given String = s"$bikeId: "
      ride.status match
        case RideStatus.BikeGoingToUser(junctionId) =>
          if waitForStateChange then ()
          else
            ridePath(getBestPath(chargingStation.id, junctionId))
            quickRequest
              .post(uri"$ridesUri/${id.value}/eBikeArrivedToUser")
              .sendSyncLogError(retryUntilSuccessInterval = 500)
            waitForStateChange = true
        case RideStatus.UserRiding =>
          // simulate random riding
          ridePath(Random.shuffle(currentJunction.streets.toSeq).take(1))
        case RideStatus.BikeGoingBackToStation =>
          ridePath(getBestPath(currentJunction.id, chargingStation.id))
          quickRequest
            .post(uri"$ridesUri/${id.value}/eBikeReachedStation")
            .sendSyncLogError(retryUntilSuccessInterval = 500)
          setActiveRides(_ - id)
          return // exits function (terminating the thread)
      Thread.sleep(100)

private object Utils:
  import scala.concurrent.*
  import sttp.model.MediaType

  extension [T](r: Request[T])
    def jsonBody[U: ReadWriter](body: U): Request[T] =
      r.body(write(body)).contentType(MediaType.ApplicationJson)

  extension (r: Request[String])
    def sendSyncLogError(retryUntilSuccessInterval: Long = 0)(using
        logPrefix: String = ""
    ): Option[String] =
      var result = Option.empty[String]
      while
        result = (for
          response <- Try(r.send(DefaultSyncBackend()))
          result = response match
            case res if res.isSuccess => Some(res.body)
            case res =>
              println(
                s"${logPrefix}${r.method} request ${r.uri} failed with status ${res.code}: ${res.body}"
              )
              None
        yield (result)) match
          case Success(value) => value
          case Failure(t) =>
            println(
              s"${logPrefix}${r.method} request ${r.uri} failed with exception: $t"
            )
            None
        if result == None && retryUntilSuccessInterval != 0 then
          println(
            s"${logPrefix}retrying in ${retryUntilSuccessInterval}ms"
          )
          Thread.sleep(retryUntilSuccessInterval)
        result.isEmpty && retryUntilSuccessInterval != 0
      do ()
      result

object ABikesSimulator:
  private case class EBikeId(value: String) derives ReadWriter

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

  enum EBikeLocation derives ReadWriter:
    case Junction(id: JunctionId)
    case Street(id: StreetId)
  case class UpdateEBikeLocationDTO(location: EBikeLocation) derives ReadWriter

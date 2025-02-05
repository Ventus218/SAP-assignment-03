package ebikes

import scala.util.Random
import sttp.client4.*
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

  def run(): Unit =
    // getting streets graph
    quickRequest
      .get(uri"$smartCityUri/junctions")
      .send(DefaultSyncBackend()) match
      case res if res.code.isSuccess =>
        junctions = read[Seq[Junction]](res.body).map(j => (j.id -> j)).toMap
      case res =>
        println(
          s"Unable to get streets graph, status ${res.code}: ${res.body}"
        ) // log error
    while true do
      quickRequest
        .get(uri"$ridesUri/active")
        .send(DefaultSyncBackend()) match
        case res if res.code.isSuccess =>
          val rides = read[Set[Ride]](res.body)
          val newRides = rides.map(_.id) -- activeRides.keySet
          setActiveRides(_ ++ rides.map(r => (r.id -> r)))
          newRides.foreach(id =>
            Thread.ofVirtual().start(() => rideSimulator(id))
          )
        case res =>
          println(s"Status ${res.code}: ${res.body}") // log error

      Thread.sleep(1000)

  private def rideSimulator(id: RideId): Unit =
    var waitForStateChange = false
    var currentJunction = chargingStation

    def ridePath(logPrefix: String, from: JunctionId, to: JunctionId): Unit =
      quickRequest
        .get(uri"$smartCityUri/path?from=${from.value}&to=${to.value}")
        .send(DefaultSyncBackend()) match
        case res if res.isSuccess =>
          val path = read[Seq[Street]](res.body)
          path.foreach: street =>
            currentJunction.semaphore match
              case None => ()
              case Some(Semaphore(SemaphoreId(id), _, _, _, _)) =>
                quickRequest
                  .get(uri"$smartCityUri/semaphores/$id")
                  .send(DefaultSyncBackend()) match
                  case res if res.isSuccess =>
                    val sem = read[Semaphore](res.body)
                    print(s"$logPrefix: Semaphore $id is ${sem.state}")
                    val waitTime =
                      if (sem.state == SemaphoreState.Red)
                        print(
                          s", wait for ${sem.nextChangeStateTimestamp}ms"
                        )
                        Thread.sleep(sem.nextChangeStateTimestamp)
                      println("")
                  case res =>
                    println(
                      s"Failed best path request, status ${res.code}: ${res.body}"
                    ) // log error
            println(s"$logPrefix: Going through street ${street.id.value}")
            Thread.sleep(street.timeLengthMillis)
        case res =>
          println(
            s"Failed best path request, status ${res.code}: ${res.body}"
          ) // log error
    while true do
      val ride = activeRides(id)
      val bikeId = ride.eBikeId.value
      ride.status match
        case RideStatus.BikeGoingToUser(junctionId) =>
          if waitForStateChange then ()
          else
            ridePath(bikeId, chargingStation.id, junctionId)
            println(s"$bikeId: Arrived to user!")
            quickRequest
              .put(uri"$ridesUri/${id.value}/eBikeArrivedToUser")
              .send(DefaultSyncBackend())
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
          ridePath(bikeId, currentJunction.id, chargingStation.id)
          quickRequest
            .put(uri"$ridesUri/${id.value}/eBikeReachedStation")
            .send(DefaultSyncBackend())
          setActiveRides(_ - id)
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

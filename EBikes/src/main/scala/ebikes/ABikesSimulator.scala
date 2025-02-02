package ebikes

import sttp.client4.*
import upickle.default.*
import ebikes.domain.model.*

object ABikesSimulator:
  private case class RideId(value: String) derives ReadWriter

  private case class Ride(id: RideId, eBikeId: EBikeId, status: RideStatus)
      derives ReadWriter

  import java.util.Date
  enum RideStatus derives ReadWriter:
    case BikeGoingToUser
    case UserRiding
    case BikeGoingBackToStation

  given ReadWriter[EBikeId] = ReadWriter.derived
  given ReadWriter[Date] =
    readwriter[Long].bimap(
      date => date.getTime(),
      long => Date(long)
    )

class ABikesSimulator(ridesServiceAddress: String) extends Runnable:
  import Utils.*
  import ABikesSimulator.*

  private var _activeRides: Map[RideId, Ride] = Map()
  private def activeRides: Map[RideId, Ride] = synchronized(_activeRides)
  private def setActiveRides(f: Map[RideId, Ride] => Map[RideId, Ride]) =
    synchronized(this._activeRides = f(this._activeRides))

  private val ridesUri = s"http://$ridesServiceAddress/rides"

  def run(): Unit =
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
    while true do
      val ride = activeRides(id)
      ride.status match
        case RideStatus.BikeGoingToUser =>
          if waitForStateChange then ()
          else
            // TODO: autonomously ride to user
            Thread.sleep(5000)
            quickRequest
              .put(uri"$ridesUri/${id.value}/eBikeArrivedToUser")
              .send(DefaultSyncBackend())
            waitForStateChange = true
        case RideStatus.UserRiding =>
          // TODO: simulate random riding
          Thread.sleep(5000)
        case RideStatus.BikeGoingBackToStation =>
          // TODO: autonomously ride to station
          Thread.sleep(5000)
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

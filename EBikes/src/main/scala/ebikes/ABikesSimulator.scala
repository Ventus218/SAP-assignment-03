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

  def run(): Unit =
    while true do
      quickRequest
        .get(uri"http://$ridesServiceAddress/rides/active")
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
    var oldStatus = Option.empty[RideStatus]
    while true do
      val ride = activeRides(id)
      ride.status match
        case RideStatus.BikeGoingToUser =>
          // TODO: autonomously ride to user
          Thread.sleep(5000)
        // TODO: inform rides service when user is reached
        case RideStatus.UserRiding =>
          Thread.sleep(5000)
        // TODO: simulate random riding
        case RideStatus.BikeGoingBackToStation =>
          // TODO: autonomously ride to station
          // TODO: inform rides service when station is reached
          // removes itself from activeRides
          setActiveRides(_ - id)
      oldStatus = Some(ride.status)

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

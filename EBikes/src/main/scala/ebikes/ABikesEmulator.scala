package ebikes

import sttp.client4.*
import sttp.model.MediaType
import upickle.default.*
import ebikes.domain.model.*

object ABikesEmulator:
  private case class RideId(value: String) derives ReadWriter

  private case class Ride(id: RideId, eBikeId: EBikeId, status: RideStatus)
      derives ReadWriter

  import java.util.Date
  enum RideStatus derives ReadWriter:
    case BikeGoingToUser
    case UserRiding
    case BikeGoingBackToStation
    case Ended(timestamp: Date)

  given ReadWriter[EBikeId] = ReadWriter.derived
  given ReadWriter[Date] =
    readwriter[Long].bimap(
      date => date.getTime(),
      long => Date(long)
    )

class ABikesEmulator(ridesServiceAddress: String) extends Runnable:
  import Utils.*
  import ABikesEmulator.*

  private var activeRides: Map[RideId, Ride] = Map()

  def run(): Unit =
    while true do
      activeRides = quickRequest
        .get(uri"http://$ridesServiceAddress/rides/active")
        .send(DefaultSyncBackend()) match
        case res if res.code.isSuccess =>
          val rides = read[List[Ride]](res.body)
          rides.groupMapReduce(_.id)(identity)((r1, r2) => r1)
        case res =>
          println(s"Status ${res.code}: ${res.body}") // log error
          activeRides // fall back to current

      // TODO: for each ride (and bike respectively) implement behavior

private object Utils:
  import scala.concurrent.*

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

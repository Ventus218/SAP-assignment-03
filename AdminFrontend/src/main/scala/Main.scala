import scala.concurrent.*
import scala.util.Try
import scala.swing._
import scala.swing.Swing.*
import scala.swing.event._
import sttp.client4.*
import upickle.default.*
import ExecutionContext.Implicits.global
import sharedfrontend.Utils.*
import sharedfrontend.dto.*
import sharedfrontend.LoginRegisterFrame

object SwingApp extends SimpleSwingApplication {

  // Login/Register Window
  def top: Frame = LoginRegisterFrame((username, token) => {
    val homeWindow = new HomeFrame(username, token)
    homeWindow.visible = true
  })

  // Home Window
  class HomeFrame(private val username: Username, token: String) extends Frame {
    private var authToken: Option[String] = Some(token)
    private var eBikes: Seq[EBike] = Seq()
    private var users: Seq[User] = Seq()
    private var rides: Seq[Ride] = Seq()
    private var counters: Seq[CounterDTO] = Seq()
    private var endpoints: Seq[MonitoredEndpointDTO] = Seq()

    title = "Home"

    val textArea = new TextArea {
      editable = false
      preferredSize = new Dimension(400, 1000)
    }

    contents = new BoxPanel(Orientation.Vertical) {
      contents += textArea

      border = Swing.EmptyBorder(10, 10, 10, 10)
    }

    updateUI()
    startPolling()

    private def updateUI(): Unit =
      val eBikesText = "EBikes:\n" + eBikes.foldLeft("")((s, b) => s + s"""
        id: ${b.id.value}
          location: ${b.location.x}, ${b.location.y}
          direction: ${b.direction.x}, ${b.direction.y}
          speed: ${b.speed}
        """.strip() + "\n")
      val usersText = "Users:\n" + users.foldLeft("")((s, u) => s + s"""
        username: ${u.username.value}
          credits: ${u.credit.amount}
        """.strip() + "\n")
      val ridesText = "Rides:\n" + rides.foldLeft("")((s, r) => s + s"""
        id: ${r.id.value}
          username: ${r.username.value}
          eBike: ${r.eBikeId.value}
        """.strip() + "\n")
      val countersText =
        "Metrics:\n" + counters.foldLeft("")((s, c) => s + s"""
        name: ${c.id.value}
          value: ${c.value}
        """.strip() + "\n")
      val endpointsText =
        "Service health status:\n" + endpoints.foldLeft("")((s, e) => s + s"""
        name: ${e.endpoint.value}
          status: ${e.status}
        """.strip() + "\n")
      textArea.text =
        s"$eBikesText\n$usersText\n$ridesText\n\n$countersText\n$endpointsText"

    private def fetchData(): Unit =
      fetchUsers().map(res =>
        onEDT:
          res match
            case Left(value)  => Dialog.showMessage(this, value)
            case Right(users) => this.users = users
          updateUI()
      )
      fetchEBikes().map(res =>
        onEDT:
          res match
            case Left(value)   => Dialog.showMessage(this, value)
            case Right(eBikes) => this.eBikes = eBikes
          updateUI()
      )
      fetchRides().map(res =>
        onEDT:
          res match
            case Left(value)  => Dialog.showMessage(this, value)
            case Right(rides) => this.rides = rides
          updateUI()
      )
      fetchCounters().map(res =>
        onEDT:
          res match
            case Left(value)     => Dialog.showMessage(this, value)
            case Right(counters) => this.counters = counters
          updateUI()
      )
      fetchEndpoints().map(res =>
        onEDT:
          res match
            case Left(value)      => Dialog.showMessage(this, value)
            case Right(endpoints) => this.endpoints = endpoints
          updateUI()
      )

    private def fetchUsers(): Future[Either[String, Seq[User]]] =
      for
        res <- quickRequest
          .get(
            uri"http://localhost:8082/users"
          ) // TODO: move to api gateway
          .authorizationBearer(authToken.get)
          .sendAsync()
        users =
          for
            res <- res
            users <- Either.cond(
              res.isSuccess,
              read[Seq[User]](res.body),
              res.body
            )
          yield (users)
      yield (users)

    private def fetchEBikes(): Future[Either[String, Seq[EBike]]] =
      for
        res <- quickRequest
          .get(uri"http://localhost:8080/ebikes")
          .authorizationBearer(authToken.get)
          .sendAsync()
        eBikes =
          for
            res <- res
            eBikes <- Either.cond(
              res.isSuccess,
              read[Seq[EBike]](res.body),
              res.body
            )
          yield (eBikes)
      yield (eBikes)

    private def fetchRides(): Future[Either[String, Seq[Ride]]] =
      for
        res <- quickRequest
          .get(uri"http://localhost:8083/rides/active")
          .authorizationBearer(authToken.get)
          .sendAsync()
        rides =
          for
            res <- res
            rides <- Either.cond(
              res.isSuccess,
              read[Seq[Ride]](res.body),
              res.body
            )
          yield (rides)
      yield (rides)

    private def fetchCounters(): Future[Either[String, Seq[CounterDTO]]] =
      for
        res <- quickRequest
          .get(uri"http://localhost:8085/metrics/counters")
          .authorizationBearer(authToken.get)
          .sendAsync()
        counters =
          for
            res <- res
            counters <- Either.cond(
              res.isSuccess,
              read[Seq[CounterDTO]](res.body),
              res.body
            )
          yield (counters)
      yield (counters)

    private def fetchEndpoints()
        : Future[Either[String, Seq[MonitoredEndpointDTO]]] =
      for
        res <- quickRequest
          .get(uri"http://localhost:8085/metrics/endpoints")
          .authorizationBearer(authToken.get)
          .sendAsync()
        endpoints =
          for
            res <- res
            _ = println(res)
            endpoints <- Either.cond(
              res.isSuccess,
              read[Seq[MonitoredEndpointDTO]](res.body),
              res.body
            )
          yield (endpoints)
      yield (endpoints)

    // POLLING
    given ExecutionContext = ExecutionContext.fromExecutor(
      java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()
    )

    private def startPolling(): Future[Unit] =
      fetchData()
      rescheduleAfterMillis(500)

    private def rescheduleAfterMillis(delay: Long): Future[Unit] =
      for
        _ <- Future(Thread.sleep(delay))
        _ <- startPolling()
      yield ()
  }
}

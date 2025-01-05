package shared.adapters.presentation
import akka.http.scaladsl.server.ExceptionHandler
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes.*

object ExceptionHandlers:
  val log = ExceptionHandler({ case e =>
    complete(InternalServerError, e.toString())
  })
  val production = ExceptionHandler({ case _ => complete(InternalServerError) })

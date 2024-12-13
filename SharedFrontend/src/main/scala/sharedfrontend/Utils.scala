package sharedfrontend

import scala.concurrent.Future
import sttp.client4.*
import upickle.default.*
import sttp.model.MediaType
import scala.concurrent.ExecutionContext

object Utils:
  extension [T](r: Request[T])
    def jsonBody(body: String): Request[T] =
      r.body(body).contentType(MediaType.ApplicationJson)

    def jsonBody[U: ReadWriter](body: U): Request[T] =
      r.body(write(body)).contentType(MediaType.ApplicationJson)

    def authorizationBearer(token: String): Request[T] =
      r.header("Authorization", s"Bearer $token")

    def sendAsync()(using
        ExecutionContext
    ): Future[Either[String, Response[T]]] =
      r.send(DefaultFutureBackend())
        .map(Right(_))
        .recover({ case t: Throwable =>
          Left(t.getMessage())
        })

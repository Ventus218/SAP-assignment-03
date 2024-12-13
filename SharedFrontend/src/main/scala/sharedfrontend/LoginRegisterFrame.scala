package sharedfrontend

import scala.swing._
import scala.swing.Swing.*
import scala.swing.event._
import sttp.client4.*
import upickle.default.*
import sharedfrontend.Utils.*
import sharedfrontend.dto.*
import scala.concurrent.*
import ExecutionContext.Implicits.global

class LoginRegisterFrame(onSuccess: (Username, String) => Unit)
    extends MainFrame {
  title = "Login/Register"

  val usernameField = new TextField { columns = 15 }
  val passwordField = new PasswordField { columns = 15 }
  val loginButton = new Button("Login")
  val registerButton = new Button("Register")

  contents = new BoxPanel(Orientation.Vertical) {
    contents += new BoxPanel(Orientation.Horizontal) {
      contents += new Label("Username:")
      contents += usernameField
    }
    contents += new BoxPanel(Orientation.Horizontal) {
      contents += new Label("Password:")
      contents += passwordField
    }
    contents += new BoxPanel(Orientation.Horizontal) {
      contents += loginButton
      contents += registerButton
    }
    border = Swing.EmptyBorder(10, 10, 10, 10)
  }

  listenTo(loginButton, registerButton)

  reactions += {
    case ButtonClicked(`loginButton`) =>
      val username = usernameField.text
      val password = passwordField.password.mkString
      if (username.nonEmpty && password.nonEmpty) {
        for
          token <- login(username, password)
          _ = onEDT:
            token match
              case Left(error) => Dialog.showMessage(this, error)
              case Right(token) =>
                onSuccess(Username(username), token)
                this.close()
        yield ()
      } else {
        Dialog.showMessage(this, "Please enter valid credentials.")
      }

    case ButtonClicked(`registerButton`) =>
      val username = usernameField.text
      val password = passwordField.password.mkString
      if (username.nonEmpty && password.nonEmpty) {
        for
          token <- register(username, password)
          _ = onEDT:
            token match
              case Left(error) => Dialog.showMessage(this, error)
              case Right(token) =>
                onSuccess(Username(username), token)
                this.close()
        yield ()
      } else {
        Dialog.showMessage(this, "Please enter valid credentials.")
      }
  }

  private def login(
      username: String,
      password: String
  ): Future[Either[String, String]] =
    for
      res <- quickRequest
        .post(
          uri"http://localhost:8080/authentication/$username/authenticate"
        )
        .jsonBody(AuthenticateDTO(password))
        .sendAsync()
      token =
        for
          res <- res
          token <- Either.cond(res.isSuccess, res.body, res.body)
        // TODO: set timer for refresh
        yield (token)
    yield (token)

  private def register(
      username: String,
      password: String
  ): Future[Either[String, String]] =
    for
      res <- quickRequest
        .post(
          uri"http://localhost:8080/authentication/register"
        )
        .jsonBody(RegisterDTO(Username(username), password))
        .sendAsync()
      token =
        for
          res <- res
          token <- Either.cond(res.isSuccess, res.body, res.body)
        // TODO: set timer for refresh
        yield (token)
    yield (token)
}

package rides.domain.errors

final case class FailureInOtherService(
    message: String =
      "Something went wrong while interacting with another service"
)

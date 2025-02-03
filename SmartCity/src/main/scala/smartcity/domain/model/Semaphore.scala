package smartcity.domain.model

final case class SemaphoreId(value: String)
final case class Semaphore(
    id: SemaphoreId,
    state: SemaphoreState,
    timeGreenMillis: Long,
    timeRedMillis: Long,
    nextChangeStateTimestamp: Long
)

enum SemaphoreState:
  case Red
  case Green

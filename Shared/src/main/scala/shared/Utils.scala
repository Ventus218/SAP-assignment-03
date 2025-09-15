package shared

object Utils:
  def combineHealthCheckErrors(healthChecks: Option[String]*): Option[String] =
    healthChecks.flatMap(err => err.toSeq) match
      case Nil      => None
      case h :: Nil => Some(h)
      case errors   => Some(s"Errors:  ${errors.mkString("\n")}")

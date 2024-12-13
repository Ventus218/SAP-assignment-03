package rides.ports

import scala.concurrent.Future
import rides.domain.model.*

trait UsersService:
  def exist(username: Username): Future[Boolean]

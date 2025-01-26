package rides.ports

import rides.domain.model.*

trait UsersService:
  def exists(username: Username, atTimestamp: Long = Long.MaxValue): Boolean

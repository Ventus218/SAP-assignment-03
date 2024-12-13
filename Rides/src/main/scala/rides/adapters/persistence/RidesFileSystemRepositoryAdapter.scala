package rides.adapters.persistence

import java.util.Date;
import rides.domain.model.*
import rides.ports.persistence.RidesRepository
import shared.adapters.persistence.FileSystemRepositoryAdapter
import shared.technologies.persistence.FileSystemDatabase
import upickle.default.*
import shared.ports.persistence.exceptions.NotInRepositoryException
import shared.ports.persistence.exceptions.DuplicateIdException

class RidesFileSystemRepositoryAdapter(db: FileSystemDatabase)
    extends RidesRepository:

  given ReadWriter[Date] =
    readwriter[Long].bimap(
      date => date.getTime(),
      long => Date(long)
    )

  given ReadWriter[Username] = ReadWriter.derived
  given ReadWriter[EBikeId] = ReadWriter.derived
  given ReadWriter[RideId] = ReadWriter.derived
  given ReadWriter[Ride] = ReadWriter.derived

  private[RidesFileSystemRepositoryAdapter] val repo =
    FileSystemRepositoryAdapter[RideId, Ride](db, "rides")

  export repo.*

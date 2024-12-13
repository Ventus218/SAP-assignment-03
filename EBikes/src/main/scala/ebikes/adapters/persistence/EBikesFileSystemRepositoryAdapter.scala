package ebikes.adapters.persistence

import ebikes.domain.model.*
import ebikes.ports.persistence.EBikesRepository
import shared.adapters.persistence.FileSystemRepositoryAdapter
import shared.technologies.persistence.FileSystemDatabase
import upickle.default.*

class EBikesFileSystemRepositoryAdapter(db: FileSystemDatabase)
    extends EBikesRepository:

  given ReadWriter[V2D] = ReadWriter.derived
  given ReadWriter[EBikeId] = ReadWriter.derived
  given ReadWriter[EBike] = ReadWriter.derived

  val repo = FileSystemRepositoryAdapter[EBikeId, EBike](db, "ebikes")

  export repo.*

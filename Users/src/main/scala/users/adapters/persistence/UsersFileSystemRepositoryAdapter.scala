package users.adapters.persistence

import users.domain.model.*
import users.ports.persistence.UsersRepository
import shared.adapters.persistence.FileSystemRepositoryAdapter
import shared.technologies.persistence.FileSystemDatabase
import upickle.default.*

class UsersFileSystemRepositoryAdapter(db: FileSystemDatabase)
    extends UsersRepository:

  given ReadWriter[Credit] = ReadWriter.derived
  given ReadWriter[Username] = ReadWriter.derived
  given ReadWriter[User] = ReadWriter.derived

  val repo = FileSystemRepositoryAdapter[Username, User](db, "users")

  export repo.*

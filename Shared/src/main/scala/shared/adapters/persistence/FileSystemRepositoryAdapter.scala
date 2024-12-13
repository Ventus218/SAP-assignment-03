package shared.adapters.persistence;

import java.io.*;
import java.nio.file.Files;
import scala.util.*
import upickle.default._
import shared.ports.persistence.exceptions.*;
import shared.ports.persistence.Repository;
import shared.technologies.persistence.FileSystemDatabase;

class FileSystemRepositoryAdapter[ID: ReadWriter, T: ReadWriter](
    private val db: FileSystemDatabase,
    private val entityName: String
) extends Repository[ID, T]:

  case class Item(id: ID, obj: T) derives ReadWriter

  private val filename = entityName + ".json"

  private val file = Try(db.getFile(filename)) match
    case Success(value) => value
    case Failure(exception) =>
      (Try:
        var f = db.createFile(filename);
        Files.write(f.toPath(), write[Seq[Item]](Seq()).getBytes());
        f
      ) match
        case Success(value) => value
        case Failure(exception) =>
          throw new RuntimeException(
            "Something went wrong while creating file " + filename,
            exception
          );

  private val transactionLock = java.util.concurrent.locks.ReentrantLock(true)

  private def getAllItems(): Seq[Item] =
    read[Seq[Item]](String(Files.readAllBytes(file.toPath())))

  private def overwriteItems(items: Seq[Item]): Unit =
    Files.write(file.toPath(), write(items).getBytes())

  override def transaction[T](f: => T): T =
    try {
      transactionLock.lock()
      f
    } finally {
      transactionLock.unlock()
    }

  override def getAll(): Iterable[T] =
    transaction:
      getAllItems().map(_.obj)

  override def insert(id: ID, entity: T): Either[DuplicateIdException, Unit] =
    transaction:
      getAllItems().exists(_.id == id) match
        case true =>
          Left(
            DuplicateIdException(s"An $entityName with id $id already exists.")
          )
        case false =>
          overwriteItems(Item(id, entity) +: getAllItems())
          Right(())

  override def delete(id: ID): Either[NotInRepositoryException, Unit] =
    transaction:
      getAllItems().exists(_.id == id) match
        case false =>
          Left(
            NotInRepositoryException(
              s"An $entityName with id $id does not exist."
            )
          )
        case true =>
          overwriteItems(getAllItems().filter(_.id != id))
          Right(())

  override def find(id: ID): Option[T] =
    transaction:
      getAllItems().find(_.id == id).map(_.obj)

  override def update(
      id: ID,
      f: T => T
  ): Either[NotInRepositoryException, T] =
    transaction:
      find(id) match
        case None =>
          Left(
            NotInRepositoryException(
              s"An $entityName with id $id does not exist."
            )
          )
        case Some(obj) =>
          val updated = f(obj)
          overwriteItems(getAllItems().collect {
            case Item(`id`, _) => Item(id, updated)
            case item          => item
          })
          Right(updated)

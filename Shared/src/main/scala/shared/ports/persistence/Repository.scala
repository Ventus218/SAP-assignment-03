package shared.ports.persistence;

import shared.ports.persistence.exceptions.*;

trait Repository[ID, T] {

  def transaction[T](f: => T): T

  def getAll(): Iterable[T]

  def insert(id: ID, entity: T): Either[DuplicateIdException, Unit]

  def update(id: ID, f: T => T): Either[NotInRepositoryException, T]

  def find(id: ID): Option[T]

  def delete(id: ID): Either[NotInRepositoryException, Unit]
}

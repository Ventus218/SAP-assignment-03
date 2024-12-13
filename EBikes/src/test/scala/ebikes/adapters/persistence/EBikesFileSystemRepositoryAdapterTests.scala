package ebikes.domain

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.*
import shared.ports.persistence.exceptions.*
import shared.technologies.persistence.FileSystemDatabaseImpl
import ebikes.domain.model.*
import ebikes.domain.errors.*
import ebikes.ports.persistence.EBikesRepository
import ebikes.adapters.persistence.EBikesFileSystemRepositoryAdapter
import better.files._
import File._

class EBikesFileSystemRepositoryAdapterTests extends AnyFlatSpec:
  trait CleanService:
    val tempDir = File.temporaryDirectory().get()
    val db = FileSystemDatabaseImpl(tempDir.toJava)
    val repo = new EBikesFileSystemRepositoryAdapter(db)

  trait DoubleBikeService extends CleanService:
    val bikeIds = Set[EBikeId]("b1", "b2")
    val bikes = bikeIds.map(EBike(_, V2D(), V2D(), 0))
    bikes.foreach(b => repo.insert(b.id, b))

  "getAll" should "retrieve no bike if repo is empty" in new CleanService:
    repo.getAll().isEmpty shouldBe true

  it should "retrieve every inserted bike" in new DoubleBikeService:
    repo.getAll() should contain theSameElementsAs bikes

  "insert" should "add a bike to the repository" in new CleanService:
    val id = EBikeId("b1")
    val bike = EBike(id, V2D(), V2D(), 0)
    repo.insert(id, bike)
    repo.find(id) shouldBe Some(bike)

  it should "not add a bike to the repository if the id already exists" in new DoubleBikeService:
    val bike = bikes.head
    val res = repo.insert(bike.id, bike)
    res.isLeft shouldBe true
    res.left.get shouldBe a[DuplicateIdException]

  "delete" should "remove a bike from the repo" in new DoubleBikeService:
    repo.delete(bikeIds.head)
    repo.getAll().size shouldBe 1

  it should "return an error if no bike with that id was found" in new CleanService:
    val res = repo.delete("blabla")
    res.isLeft shouldBe true
    res.left.get shouldBe a[NotInRepositoryException]

  private given Conversion[String, EBikeId] with
    def apply(x: String): EBikeId = EBikeId(x)

package ebikes.domain

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.*
import shared.adapters.persistence.InMemoryRepositoryAdapter
import shared.technologies.persistence.InMemoryMapDatabaseImpl
import ebikes.domain.model.*
import ebikes.domain.errors.*
import ebikes.ports.persistence.EBikesRepository

class EBikesServiceTests extends AnyFlatSpec:
  trait CleanService:
    val db = InMemoryMapDatabaseImpl()
    val repo = new InMemoryRepositoryAdapter[EBikeId, EBike](db, "ebikes")
      with EBikesRepository
    val service = EBikesServiceImpl(repo)

  trait DoubleBikeService extends CleanService:
    val bikeIds = Set[EBikeId]("b1", "b2")
    bikeIds.foreach(service.register(_, V2D(), V2D()))

  "eBikes" should "retrieve no bike initially" in new CleanService:
    service.eBikes().size shouldBe 0

  it should "retrieve all registered bikes" in new DoubleBikeService:
    service.eBikes().size shouldBe bikeIds.size
    service
      .eBikes()
      .map(b => bikeIds.contains(b.id))
      .reduce(_ && _) shouldBe true

  "find" should "retrieve no bike if not present" in new DoubleBikeService:
    service.find("blabla") shouldBe None

  it should "retrieve a bike if present" in new DoubleBikeService:
    val id = bikeIds.head
    val bike = service.find(id)
    bike.isDefined shouldBe true
    bike.get.id shouldBe id

  "register" should "add a new bike" in new CleanService:
    val id = "b1"
    service.register(id, V2D(), V2D())
    service.find(id).isDefined shouldBe true

  it should "add a new bike with provided data and speed set to 0" in new CleanService:
    val id = "b1"
    val location = V2D(1, 2)
    val direction = V2D(3, 4)
    service.register(id, location, direction)
    service.find(id) shouldBe Some(EBike(id, location, direction, 0))

  it should "not allow to register a new bike with an already existing id" in new DoubleBikeService:
    val id = bikeIds.head
    val res = service.register(id, V2D(), V2D())
    res.isLeft shouldBe true
    res.left.get shouldBe a[EBikeIdAlreadyInUse]

  private given Conversion[String, EBikeId] with
    def apply(x: String): EBikeId = EBikeId(x)

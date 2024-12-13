package ebikes.domain.model

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.*

class V2DTests extends AnyFlatSpec:
  "apply" should "return a vector with x=0, y=0" in:
    V2D() shouldBe V2D()

  "two vectors sum" should "return a vector where each component is the sum of the respective vectors component" in:
    V2D(1, 2) + V2D(0, 3) shouldBe V2D(1, 5)

  "two vectors subtraction" should "return a vector where each component is the subtraction of the respective vectors component" in:
    V2D(1, 2) - V2D(0, 3) shouldBe V2D(1, -1)

  "two vectors product" should "return a vector where each component is the product of the respective vectors component" in:
    V2D(1, 2) * V2D(0, 3) shouldBe V2D(0, 6)

  "two vectors division" should "return a vector where each component is the division of the respective vectors component" in:
    V2D(1, 2) / V2D(2, 3) shouldBe V2D(0.5, 2d / 3d)

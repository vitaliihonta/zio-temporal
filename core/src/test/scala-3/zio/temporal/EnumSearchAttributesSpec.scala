package zio.temporal

import io.temporal.common.SearchAttributeKey
import org.scalatest.wordspec.AnyWordSpec

object EnumSearchAttributesSpec {
  enum Color {
    case Red, Green, Blue
  }

  enum Planet(mass: Double, radius: Double) {
    private final val G = 6.67300e-11

    def surfaceGravity = G * mass / (radius * radius)

    def surfaceWeight(otherMass: Double) = otherMass * surfaceGravity

    case Mercury extends Planet(3.303e+23, 2.4397e6)
    case Venus   extends Planet(4.869e+24, 6.0518e6)
    case Earth   extends Planet(5.976e+24, 6.37814e6)
    case Mars    extends Planet(6.421e+23, 3.3972e6)
    case Jupiter extends Planet(1.9e+27, 7.1492e7)
    case Saturn  extends Planet(5.688e+26, 6.0268e7)
    case Uranus  extends Planet(8.686e+25, 2.5559e7)
    case Neptune extends Planet(1.024e+26, 2.4746e7)
  }
}

// todo: add test cases
class EnumSearchAttributesSpec extends AnyWordSpec {
  import EnumSearchAttributesSpec._

  "ZSearchAttributeMeta" should {
    "work for scala3 enums" in {
      val meta = ZSearchAttributeMeta[Color]

      assert(meta.encode(Color.Red) == "Red")
      assert(meta.decode("Red") == Color.Red)
      assert(
        meta.attributeKey("color") == SearchAttributeKey.forKeyword("color")
      )
    }

    "work for scala3 enums with parameters" in {
      val meta = ZSearchAttributeMeta[Planet]

      assert(meta.encode(Planet.Earth) == "Earth")
      assert(meta.decode("Earth") == Planet.Earth)
      assert(
        meta.attributeKey("planet") == SearchAttributeKey.forKeyword("planet")
      )
    }
  }
}

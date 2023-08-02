package zio.temporal.enumeratum

import _root_.enumeratum.{Enum, EnumEntry}
import _root_.enumeratum.values.{StringEnum, StringEnumEntry}
import io.temporal.common.SearchAttributeKey
import org.scalatest.wordspec.AnyWordSpec
import zio.temporal.{ZSearchAttribute, ZSearchAttributeMeta}

object EnumSearchAttributesSpec {
  sealed abstract class Color(val value: String) extends StringEnumEntry
  object Color extends StringEnum[Color] {
    case object Red   extends Color("red")
    case object Green extends Color("green")
    case object Blue  extends Color("blue")

    override val values = findValues
  }

  sealed abstract class Planet(val mass: Double, val radius: Double) extends EnumEntry
  object Planet extends Enum[Planet] {
    case object Mercury extends Planet(3.303e+23, 2.4397e6)
    case object Venus   extends Planet(4.869e+24, 6.0518e6)
    case object Earth   extends Planet(5.976e+24, 6.37814e6)
    case object Mars    extends Planet(6.421e+23, 3.3972e6)
    case object Jupiter extends Planet(1.9e+27, 7.1492e7)
    case object Saturn  extends Planet(5.688e+26, 6.0268e7)
    case object Uranus  extends Planet(8.686e+25, 2.5559e7)
    case object Neptune extends Planet(1.024e+26, 2.4746e7)

    override val values = findValues
  }
}

// todo: add test cases
class EnumSearchAttributesSpec extends AnyWordSpec {
  import EnumSearchAttributesSpec._
  import zio.temporal.enumeratum

  "ZSearchAttributeMeta" should {
    "work for enumeratum string enums" in {
      val meta = ZSearchAttributeMeta[Color]

      assert(meta.encode(Color.Red) == "red")
      assert(meta.decode("red") == Color.Red)
      assert(
        meta.attributeKey("color") == SearchAttributeKey.forKeyword("color")
      )
    }

    "work for enumeratum enums with parameters" in {
      val meta = ZSearchAttributeMeta[Planet]

      assert(meta.encode(Planet.Earth) == "Earth")
      assert(meta.decode("Earth") == Planet.Earth)
      assert(
        meta.attributeKey("planet") == SearchAttributeKey.forKeyword("planet")
      )
    }
  }
}

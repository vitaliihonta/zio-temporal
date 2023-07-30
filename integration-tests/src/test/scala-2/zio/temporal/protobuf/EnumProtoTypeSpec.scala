package zio.temporal.protobuf

import com.example.testing.Color
import org.scalatest.wordspec.AnyWordSpec
import enumeratum.{Enum, EnumEntry}

object EnumProtoTypeSpec {
  sealed trait Color extends EnumEntry

  object Color extends Enum[Color] {
    case object Red          extends Color
    case object Green        extends Color
    case object Blue         extends Color
    case object ScalaInvalid extends Color

    override val values = findValues
  }
}

class EnumProtoTypeSpec extends AnyWordSpec {
  private val enumProto = EnumProtoType(Color).to(EnumProtoTypeSpec.Color)

  "EnumProtoType" should {
    "convert enumeratum enum to protobuf correctly" in {
      assert(enumProto.repr(EnumProtoTypeSpec.Color.Red) == Color.Red)
      assert(enumProto.repr(EnumProtoTypeSpec.Color.Green) == Color.Green)
      assert(enumProto.repr(EnumProtoTypeSpec.Color.Blue) == Color.Blue)
      assertThrows[EnumeratumEnumException] {
        enumProto.repr(EnumProtoTypeSpec.Color.ScalaInvalid)
      }
    }

    "convert protobuf enum to scala3 enum correctly" in {
      assert(enumProto.fromRepr(Color.Red) == EnumProtoTypeSpec.Color.Red)
      assert(enumProto.fromRepr(Color.Green) == EnumProtoTypeSpec.Color.Green)
      assert(enumProto.fromRepr(Color.Blue) == EnumProtoTypeSpec.Color.Blue)
      assertThrows[NoSuchElementException] {
        enumProto.fromRepr(Color.Invalid)
      }
    }
  }
}

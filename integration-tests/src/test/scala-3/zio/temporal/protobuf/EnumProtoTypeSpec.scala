package zio.temporal.protobuf

import org.scalatest.wordspec.AnyWordSpec
import com.example.testing.Color

object EnumProtoTypeSpec {
  enum Color {
    case Red, Green, Blue, ScalaInvalid
  }

}

class EnumProtoTypeSpec extends AnyWordSpec {
  private val enumProto = EnumProtoType(Color).to[EnumProtoTypeSpec.Color]

  "EnumProtoType" should {
    "convert scala3 enum to protobuf correctly" in {
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

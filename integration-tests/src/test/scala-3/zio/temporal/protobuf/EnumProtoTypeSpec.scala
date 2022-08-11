package zio.temporal.protobuf

import org.scalatest.wordspec.AnyWordSpec
import com.example.testing.Color

object EnumProtoTypeSpec {
  enum Color {
    case Red, Green, Blue
  }
}

class EnumProtoTypeSpec extends AnyWordSpec {
  "EnumProtoType" should {
    "convert to scala3 enum correctly" in {
      EnumProtoType(Color).to[EnumProtoTypeSpec.Color]
    }
  }
}

package zio.temporal.protobuf.internal

import org.scalatest.wordspec.AnyWordSpec
import com.example.testing.{TestingProto2Proto, TestingProtoProto}
import org.scalatest.matchers.should.Matchers

class ProtoFileObjectAutoLoaderSpec extends AnyWordSpec with Matchers {
  "ProtoFileObjectAutoLoader" should {
    "load all user generated proto files via reflection" in {
      val actualResult   = ProtoFileObjectAutoLoader.loadFromClassPath(getClass.getClassLoader)
      val expectedResult = List(TestingProtoProto, TestingProto2Proto)
      actualResult should contain theSameElementsAs expectedResult
    }
  }
}

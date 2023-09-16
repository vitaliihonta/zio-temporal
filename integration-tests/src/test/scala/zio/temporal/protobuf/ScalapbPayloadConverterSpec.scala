package zio.temporal.protobuf

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import com.example.testing._
import com.google.protobuf.ByteString
import io.temporal.api.common.v1.Payload
import io.temporal.common.converter.EncodingKeys
import org.scalatest.{Assertion, OptionValues}
import zio.temporal.JavaTypeTag

import java.nio.charset.StandardCharsets
import scala.jdk.OptionConverters._

object ScalapbPayloadConverterSpec {
  case class TemporaryClass(value: Int)
}

class ScalapbPayloadConverterSpec extends AnyWordSpec with Matchers with OptionValues {
  import ScalapbPayloadConverterSpec.TemporaryClass

  private val converter = new ScalapbPayloadConverter(
    files = List(
      TestingProtoProto,
      TestingProto2Proto,
      ProtobufTestsProto
    )
  )

  "ScalapbPayloadConverter" should {
    "(De)Serialize simple case class into protobuf bytes" in {
      val msg = TestingMessage(test = "hello")

      val payload = converter.toData(msg).toScala.value

      checkProtobufHeaders(payload)(fullName = TestingMessage.scalaDescriptor.fullName)

      val decoded = converter.fromData(payload, classOf[TestingMessage], JavaTypeTag[TestingMessage].genericType)
      decoded shouldEqual msg
    }

    "(De)Serialize oneOf" in {
      // case 1
      val msg1     = TestingOneOfMessage(value = TestingOneOfMessage.Value.First(TestingCaseFirst(42L)))
      val payload1 = converter.toData(msg1).toScala.value
      checkProtobufHeaders(payload1)(fullName = TestingOneOfMessage.scalaDescriptor.fullName)
      val decoded1 =
        converter.fromData(payload1, classOf[TestingOneOfMessage], JavaTypeTag[TestingOneOfMessage].genericType)
      decoded1 shouldEqual msg1

      // case 2
      val msg2     = TestingOneOfMessage(value = TestingOneOfMessage.Value.Second(TestingCaseSecond("baz")))
      val payload2 = converter.toData(msg2).toScala.value
      checkProtobufHeaders(payload2)(fullName = TestingOneOfMessage.scalaDescriptor.fullName)
      val decoded2 =
        converter.fromData(payload2, classOf[TestingOneOfMessage], JavaTypeTag[TestingOneOfMessage].genericType)
      decoded2 shouldEqual msg2
    }

    "(De)Serialize sealed oneOf" in {
      // case 1
      val msg1: TestingOneOfSealedMessage = TestingSealedCaseFirst("bar")
      val payload1                        = converter.toData(msg1).toScala.value
      checkProtobufHeaders(payload1)(fullName = TestingSealedCaseFirst.scalaDescriptor.fullName)

      val decoded1 =
        converter.fromData(
          payload1,
          classOf[TestingOneOfSealedMessage],
          JavaTypeTag[TestingOneOfSealedMessage].genericType
        )
      decoded1 shouldEqual msg1

      // case 2
      val msg2: TestingOneOfSealedMessage = TestingSealedCaseSecond(bar = true)
      val payload2                        = converter.toData(msg2).toScala.value
      checkProtobufHeaders(payload2)(fullName = TestingSealedCaseSecond.scalaDescriptor.fullName)

      val decoded2 =
        converter.fromData(
          payload2,
          classOf[TestingOneOfSealedMessage],
          JavaTypeTag[TestingOneOfSealedMessage].genericType
        )
      decoded2 shouldEqual msg2
    }

    "(De)Serialize Either" in {
      val msg1: Either[TestingMessage, TestingOneOfSealedMessage] = Left(TestingMessage(test = "hello"))
      val payload1                                                = converter.toData(msg1).toScala.value
      checkProtobufHeaders(payload1)(fullName = Result.scalaDescriptor.fullName)
      val decoded1 = converter.fromData(
        payload1,
        classOf[Either[TestingMessage, TestingOneOfSealedMessage]],
        JavaTypeTag[Either[TestingMessage, TestingOneOfSealedMessage]].genericType
      )
      decoded1 shouldEqual msg1

      val msg2: Either[TestingMessage, TestingOneOfSealedMessage] = Right(TestingSealedCaseSecond(bar = true))
      val payload2                                                = converter.toData(msg2).toScala.value
      checkProtobufHeaders(payload2)(fullName = Result.scalaDescriptor.fullName)
      val decoded2 = converter.fromData(
        payload2,
        classOf[Either[TestingMessage, TestingOneOfSealedMessage]],
        JavaTypeTag[Either[TestingMessage, TestingOneOfSealedMessage]].genericType
      )
      decoded2 shouldEqual msg2
    }

    "(De)Serialize Option" in {
      val msg1: Option[TestingMessage] = Some(TestingMessage(test = "hello"))
      val payload1                     = converter.toData(msg1).toScala.value
      checkProtobufHeaders(payload1)(fullName = Optional.scalaDescriptor.fullName)
      val decoded1 = converter.fromData(
        payload1,
        classOf[Option[TestingMessage]],
        JavaTypeTag[Option[TestingMessage]].genericType
      )
      decoded1 shouldEqual msg1

      val msg2: Option[TestingMessage] = None
      val payload2                     = converter.toData(msg2).toScala.value
      checkProtobufHeaders(payload2)(fullName = Optional.scalaDescriptor.fullName)
      val decoded2 = converter.fromData(
        payload2,
        classOf[Option[TestingMessage]],
        JavaTypeTag[Option[TestingMessage]].genericType
      )
      decoded2 shouldEqual msg2
    }

    "(De)Serialize Unit" in {
      val msg: Unit = ()

      val payload = converter.toData(msg).toScala.value

      checkProtobufHeaders(payload)(fullName = ZUnit.scalaDescriptor.fullName)

      val decoded = converter.fromData(payload, classOf[Unit], JavaTypeTag[Unit].genericType)
      decoded shouldEqual msg
    }

    "Not serialize non-scalapb-generated types" in {
      converter.toData("Foooo").toScala should be(empty)
      converter.toData(TemporaryClass(1)).toScala should be(empty)
    }

    "Fail to deserialize non-scalapb-generated types" in {
      val payload = Payload
        .newBuilder()
        .putMetadata(
          EncodingKeys.METADATA_MESSAGE_TYPE_KEY,
          ByteString.copyFrom("zio.temporal.protobuf.TemporaryClass", StandardCharsets.UTF_8)
        )
        .build()

      assertThrows[ProtobufPayloadException] {
        converter.fromData(
          payload,
          classOf[TemporaryClass],
          JavaTypeTag[TemporaryClass].genericType
        )
      }
    }
  }

  private def checkProtobufHeaders(payload: Payload)(fullName: String): Assertion = {
    payload.getMetadataOrThrow(EncodingKeys.METADATA_ENCODING_KEY).toStringUtf8 shouldEqual "binary/protobuf"

    payload
      .getMetadataOrThrow(EncodingKeys.METADATA_MESSAGE_TYPE_KEY)
      .toStringUtf8 shouldEqual fullName
  }
}

package zio.temporal.protobuf

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import com.example.testing._
import io.temporal.api.common.v1.Payload
import io.temporal.common.converter.EncodingKeys
import org.scalatest.{Assertion, OptionValues}
import scalapb.GeneratedMessage
import zio.temporal.JavaTypeTag

import scala.jdk.OptionConverters._

class ScalapbPayloadConverterSpec extends AnyWordSpec with Matchers with OptionValues {
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
  }

  private def checkProtobufHeaders(payload: Payload)(fullName: String): Assertion = {
    payload.getMetadataOrThrow(EncodingKeys.METADATA_ENCODING_KEY).toStringUtf8 shouldEqual "binary/protobuf"

    payload
      .getMetadataOrThrow(EncodingKeys.METADATA_MESSAGE_TYPE_KEY)
      .toStringUtf8 shouldEqual fullName
  }
}

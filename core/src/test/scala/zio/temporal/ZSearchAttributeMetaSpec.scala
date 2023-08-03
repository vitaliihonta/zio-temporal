package zio.temporal

import io.temporal.common.SearchAttributeKey
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.Tables
import ZSearchAttribute.{Keyword, Plain}

import java.time.OffsetDateTime
import java.util.UUID

class ZSearchAttributeMetaSpec extends AnyWordSpec with Tables with Matchers {
  "ZSearchAttributeMeta" should {
    val keyName = "foo"

    "encode/decode native types correctly" in {

      ZSearchAttributeMeta.string.attributeKey(keyName) shouldEqual SearchAttributeKey.forText(keyName)
      ZSearchAttributeMeta.string.encode("foo") shouldEqual Plain("foo")
      ZSearchAttributeMeta.string.decode(Plain("foo")) shouldEqual "foo"

      ZSearchAttributeMeta.stringKeyword.attributeKey(keyName) shouldEqual SearchAttributeKey.forKeyword(keyName)
      ZSearchAttributeMeta.stringKeyword.encode("foo") shouldEqual Keyword("foo")
      ZSearchAttributeMeta.stringKeyword.decode(Keyword("foo")) shouldEqual "foo"

      ZSearchAttributeMeta.long.attributeKey(keyName) shouldEqual SearchAttributeKey.forLong(keyName)
      ZSearchAttributeMeta.long.encode(123L) shouldEqual Plain(123L)
      ZSearchAttributeMeta.long.decode(Plain(123L)) shouldEqual 123L

      val odt = OffsetDateTime.now()
      ZSearchAttributeMeta.offsetDateTime.attributeKey(keyName) shouldEqual
        SearchAttributeKey.forOffsetDateTime(keyName)

      ZSearchAttributeMeta.offsetDateTime.encode(odt) shouldEqual Plain(odt)
      ZSearchAttributeMeta.offsetDateTime.decode(Plain(odt)) shouldEqual odt

      ZSearchAttributeMeta.boolean.attributeKey(keyName) shouldEqual SearchAttributeKey.forBoolean(keyName)
      ZSearchAttributeMeta.boolean.encode(true) shouldEqual Plain(true)
      ZSearchAttributeMeta.boolean.decode(Plain(true)) shouldEqual true

      ZSearchAttributeMeta.double.attributeKey(keyName) shouldEqual SearchAttributeKey.forDouble(keyName)
      ZSearchAttributeMeta.double.encode(3.1415) shouldEqual Plain(3.1415)
      ZSearchAttributeMeta.double.decode(Plain(3.1415)) shouldEqual 3.1415
    }

    "encode/decode mapped types correctly" in {
      ZSearchAttributeMeta.uuid.attributeKey(keyName) shouldEqual SearchAttributeKey.forKeyword(keyName)
      val uuid = UUID.randomUUID()

      ZSearchAttributeMeta.uuid.encode(uuid) shouldEqual Keyword(uuid.toString)
      ZSearchAttributeMeta.uuid.decode(Keyword(uuid.toString)) shouldEqual uuid
    }
  }
}

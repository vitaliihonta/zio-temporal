package zio.temporal

import io.temporal.common.SearchAttributeKey
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import java.time.{Instant, LocalDateTime, OffsetDateTime, ZoneOffset}
import java.util.UUID
import java.{util => ju}

class ZSearchAttributeMetaSpec extends AnyWordSpec with Matchers {
  "ZSearchAttributeMeta" should {
    val keyName = "foo"

    "encode/decode native types correctly" in {

      ZSearchAttributeMeta.string.attributeKey(keyName) shouldEqual SearchAttributeKey.forText(keyName)
      ZSearchAttributeMeta.string.encode("foo") shouldEqual "foo"
      ZSearchAttributeMeta.string.decode("foo") shouldEqual "foo"

      ZSearchAttributeMeta.stringKeyword.attributeKey(keyName) shouldEqual SearchAttributeKey.forKeyword(keyName)
      ZSearchAttributeMeta.stringKeyword.encode("foo") shouldEqual "foo"
      ZSearchAttributeMeta.stringKeyword.decode("foo") shouldEqual "foo"

      ZSearchAttributeMeta.long.attributeKey(keyName) shouldEqual SearchAttributeKey.forLong(keyName)
      ZSearchAttributeMeta.long.encode(123L) shouldEqual 123L
      ZSearchAttributeMeta.long.decode(123L) shouldEqual 123L

      val odt = OffsetDateTime.now()
      ZSearchAttributeMeta.offsetDateTime.attributeKey(keyName) shouldEqual
        SearchAttributeKey.forOffsetDateTime(keyName)

      ZSearchAttributeMeta.offsetDateTime.encode(odt) shouldEqual odt
      ZSearchAttributeMeta.offsetDateTime.decode(odt) shouldEqual odt

      ZSearchAttributeMeta.boolean.attributeKey(keyName) shouldEqual SearchAttributeKey.forBoolean(keyName)
      ZSearchAttributeMeta.boolean.encode(true) shouldEqual true
      ZSearchAttributeMeta.boolean.decode(true) shouldEqual true

      ZSearchAttributeMeta.double.attributeKey(keyName) shouldEqual SearchAttributeKey.forDouble(keyName)
      ZSearchAttributeMeta.double.encode(3.1415) shouldEqual 3.1415
      ZSearchAttributeMeta.double.decode(3.1415) shouldEqual 3.1415
    }

    "encode/decode mapped types correctly" in {
      ZSearchAttributeMeta.integer.attributeKey(keyName) shouldEqual SearchAttributeKey.forLong(keyName)
      ZSearchAttributeMeta.integer.encode(123) shouldEqual 123L
      ZSearchAttributeMeta.integer.decode(123L) shouldEqual 123

      ZSearchAttributeMeta.uuid.attributeKey(keyName) shouldEqual SearchAttributeKey.forKeyword(keyName)
      val uuid = UUID.randomUUID()

      ZSearchAttributeMeta.uuid.encode(uuid) shouldEqual (uuid.toString)
      ZSearchAttributeMeta.uuid.decode(uuid.toString) shouldEqual uuid

      ZSearchAttributeMeta.bigInt.attributeKey(keyName) shouldEqual SearchAttributeKey.forText(keyName)
      ZSearchAttributeMeta.bigInt.encode(BigInt(123) * 1000000000000000000L) shouldEqual "123000000000000000000"
      ZSearchAttributeMeta.bigInt.decode("123000000000000000000") shouldEqual
        (BigInt(123) * 1000000000000000000L)

      ZSearchAttributeMeta.bigDecimal.attributeKey(keyName) shouldEqual SearchAttributeKey.forText(keyName)
      ZSearchAttributeMeta.bigDecimal.encode(BigDecimal(123) * 1000000000000000000L + 0.25) shouldEqual
        "123000000000000000000.25"
      ZSearchAttributeMeta.bigDecimal.decode("123000000000000000000.25") shouldEqual
        (BigDecimal(123) * 1000000000000000000L + 0.25)

      val ldt = LocalDateTime.now()
      ZSearchAttributeMeta.localDateTime.attributeKey(keyName) shouldEqual SearchAttributeKey.forOffsetDateTime(keyName)
      ZSearchAttributeMeta.localDateTime.encode(ldt) shouldEqual (ldt.atOffset(ZoneOffset.UTC))
      ZSearchAttributeMeta.localDateTime.decode(ldt.atOffset(ZoneOffset.UTC)) shouldEqual ldt

      val instant = Instant.now()
      ZSearchAttributeMeta.instant.attributeKey(keyName) shouldEqual SearchAttributeKey.forOffsetDateTime(keyName)
      ZSearchAttributeMeta.instant.encode(instant) shouldEqual (instant.atOffset(ZoneOffset.UTC))
      ZSearchAttributeMeta.instant.decode(instant.atOffset(ZoneOffset.UTC)) shouldEqual instant

      val ctm = new ZCurrentTimeMillis(System.currentTimeMillis())
      ZSearchAttributeMeta.currentTimeMillis.attributeKey(keyName) shouldEqual
        SearchAttributeKey.forOffsetDateTime(keyName)
      ZSearchAttributeMeta.currentTimeMillis.encode(ctm) shouldEqual (ctm.toOffsetDateTime())
      ZSearchAttributeMeta.currentTimeMillis.decode(ctm.toOffsetDateTime(ZoneOffset.UTC)) shouldEqual ctm
    }

    "encode/decode containers correctly" in {
      val listMeta = ZSearchAttributeMeta[List[String], ZSearchAttribute.Keyword]

      listMeta.attributeKey(keyName) shouldEqual SearchAttributeKey.forKeywordList(keyName)
      listMeta.encode(List("hello", "world")) shouldEqual (ju.Arrays.asList("hello", "world"))
      listMeta.decode(ju.Arrays.asList("hello", "world")) shouldEqual List("hello", "world")

      val setMeta = ZSearchAttributeMeta[Set[String], ZSearchAttribute.Keyword]
      setMeta.attributeKey(keyName) shouldEqual SearchAttributeKey.forKeywordList(keyName)
      setMeta.encode(Set("hello", "world")) shouldEqual (ju.Arrays.asList("hello", "world"))
      setMeta.decode(ju.Arrays.asList("hello", "world")) shouldEqual Set("hello", "world")

      val arrayMeta = ZSearchAttributeMeta[Array[String], ZSearchAttribute.Keyword]
      arrayMeta.attributeKey(keyName) shouldEqual SearchAttributeKey.forKeywordList(keyName)
      arrayMeta.encode(Array("hello", "world")) shouldEqual (ju.Arrays.asList("hello", "world"))
      arrayMeta.decode(ju.Arrays.asList("hello", "world")) shouldEqual Array("hello", "world")

      val optionPlainMeta = ZSearchAttributeMeta.option[String, ZSearchAttribute.Plain, String]
      optionPlainMeta.attributeKey(keyName) shouldEqual SearchAttributeKey.forText(keyName)
      optionPlainMeta.encode(Some("foo")) shouldEqual "foo"
      optionPlainMeta.decode("foo") shouldEqual Some("foo")
      optionPlainMeta.encode(None) shouldEqual null
      optionPlainMeta.decode(null) shouldEqual None

      val optionKeywordMeta = ZSearchAttributeMeta.option[String, ZSearchAttribute.Keyword, String]
      optionKeywordMeta.attributeKey(keyName) shouldEqual SearchAttributeKey.forKeyword(keyName)
      optionKeywordMeta.encode(Some("foo")) shouldEqual "foo"
      optionKeywordMeta.decode("foo") shouldEqual Some("foo")
      optionKeywordMeta.encode(None) shouldEqual null
      optionKeywordMeta.decode(null) shouldEqual None
    }
  }
}

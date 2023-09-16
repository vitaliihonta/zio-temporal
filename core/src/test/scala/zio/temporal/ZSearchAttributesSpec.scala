package zio.temporal

import io.temporal.common.{SearchAttributeKey, SearchAttributes}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import zio.temporal.ZSearchAttribute.{Keyword, Plain}
import java.time.{Instant, LocalDateTime, OffsetDateTime, ZoneOffset}
import java.util.UUID
import java.{util => ju}

class ZSearchAttributesSpec extends AnyWordSpec with Matchers {
  "ZSearchAttributes.get" should {
    "return existing attribute decoded" in {
      val attrs = ZSearchAttributes.fromJava(
        SearchAttributes
          .newBuilder()
          .set(SearchAttributeKey.forText("foo"), "bar")
          .set[java.lang.Long](SearchAttributeKey.forLong("baz"), 42L)
          .set(SearchAttributeKey.forKeyword("boo"), "bzzzz")
          .set(SearchAttributeKey.forKeywordList("goods"), ju.Arrays.asList("pizza", "burger"))
          .build()
      )

      attrs.size shouldEqual 4

      attrs.containsKey[String, Plain]("foo") shouldEqual true
      attrs.get[String, Plain]("foo") shouldEqual Some("bar")

      attrs.containsKey[Long, Plain]("baz") shouldEqual true
      attrs.get[Long, Plain]("baz") shouldEqual Some(42L)

      attrs.containsKey[String, Keyword]("boo") shouldEqual true
      attrs.get[String, Keyword]("boo") shouldEqual Some("bzzzz")

      attrs.containsKey[List[String], Keyword]("goods") shouldEqual true
      attrs.get[List[String], Keyword]("goods") shouldEqual Some(List("pizza", "burger"))

      attrs.containsKey[Set[String], Keyword]("missing") shouldEqual false
      attrs.get[Set[String], Keyword]("missing") shouldEqual None
    }
  }
}

package zio.temporal

import io.temporal.common.SearchAttributeKey
import java.time.{Instant, LocalDateTime, OffsetDateTime, ZoneOffset}
import java.util.UUID
import java.{util => ju}
import scala.jdk.CollectionConverters._

// todo: cover with tests
sealed trait ZSearchAttributeMeta[A] { self =>
  type Repr

  def attributeKey(name: String): SearchAttributeKey[Repr]

  def encode(value: A): Repr

  def decode(value: Repr): A

  def convert[B](project: A => B, reverse: B => A): ZSearchAttributeMeta.Of[B, self.Repr] =
    new ZSearchAttributeMeta[B] {
      override type Repr = self.Repr

      override def attributeKey(name: String): SearchAttributeKey[Repr] =
        self.attributeKey(name)

      override def encode(value: B): Repr =
        self.encode(reverse(value))

      override def decode(value: Repr): B =
        project(self.decode(value))
    }

}

object ZSearchAttributeMeta extends ZSearchAttributeMetaVersionSpecific {
  type Of[A, R] = ZSearchAttributeMeta[A] { type Repr = R }

  def apply[A](implicit meta: ZSearchAttributeMeta[A]): meta.type = meta

  val stringKeyword: ZSearchAttributeMeta.Of[String, String] =
    new KeywordMeta[String](identity, identity)

  implicit val string: ZSearchAttributeMeta.Of[String, String]         = StringMeta
  implicit val boolean: ZSearchAttributeMeta.Of[Boolean, Boolean]      = BooleanMeta
  implicit val long: ZSearchAttributeMeta.Of[Long, Long]               = LongMeta
  implicit val integer: ZSearchAttributeMeta.Of[Int, Long]             = long.convert(_.toInt, _.toLong)
  implicit val double: ZSearchAttributeMeta.Of[Double, Double]         = DoubleMeta
  implicit val bigInt: ZSearchAttributeMeta.Of[BigInt, String]         = string.convert(BigInt(_), _.toString)
  implicit val bigDecimal: ZSearchAttributeMeta.Of[BigDecimal, String] = string.convert(BigDecimal(_), _.toString)

  implicit val uuid: ZSearchAttributeMeta.Of[UUID, String] =
    stringKeyword.convert(UUID.fromString, _.toString)

  implicit val offsetDateTime: ZSearchAttributeMeta.Of[OffsetDateTime, OffsetDateTime] =
    OffsetDateTimeMeta

  implicit val localDateTime: ZSearchAttributeMeta.Of[LocalDateTime, OffsetDateTime] =
    offsetDateTime.convert(_.toLocalDateTime, _.atOffset(ZoneOffset.UTC))

  implicit val instant: ZSearchAttributeMeta.Of[Instant, OffsetDateTime] =
    offsetDateTime.convert(_.toInstant, _.atOffset(ZoneOffset.UTC))

  implicit def keywordList[V](
    implicit asString: ZSearchAttributeMeta.Of[V, String]
  ): ZSearchAttributeMeta.Of[List[V], ju.List[String]] =
    new KeywordListMeta[V](asString)

  private[zio] sealed trait SimpleMeta[A] extends ZSearchAttributeMeta[A] {
    override final type Repr = A

    override def encode(value: A): A = value

    override def decode(value: A): A = value
  }

  private final object StringMeta extends SimpleMeta[String] {
    override def attributeKey(name: String): SearchAttributeKey[String] = {
      SearchAttributeKey.forText(name)
    }
  }

  private final object BooleanMeta extends SimpleMeta[Boolean] {
    override def attributeKey(name: String): SearchAttributeKey[Boolean] = {
      // safe to cast java.lang.Boolean to Boolean
      SearchAttributeKey.forBoolean(name).asInstanceOf[SearchAttributeKey[Boolean]]
    }
  }

  private final object LongMeta extends SimpleMeta[Long] {
    override def attributeKey(name: String): SearchAttributeKey[Long] = {
      // safe to cast java.lang.Long to Long
      SearchAttributeKey.forLong(name).asInstanceOf[SearchAttributeKey[Long]]
    }
  }

  private final object DoubleMeta extends SimpleMeta[Double] {
    override def attributeKey(name: String): SearchAttributeKey[Double] = {
      // safe to cast java.lang.Double to Double
      SearchAttributeKey.forDouble(name).asInstanceOf[SearchAttributeKey[Double]]
    }
  }

  private final object OffsetDateTimeMeta extends SimpleMeta[OffsetDateTime] {
    override def attributeKey(name: String): SearchAttributeKey[OffsetDateTime] = {
      SearchAttributeKey.forOffsetDateTime(name)
    }
  }

  final class KeywordMeta[V](encodeValue: V => String, decodeValue: String => V) extends ZSearchAttributeMeta[V] {
    override type Repr = String

    override def attributeKey(name: String): SearchAttributeKey[String] = {
      SearchAttributeKey.forKeyword(name)
    }

    override def decode(value: String): V = decodeValue(value)

    override def encode(value: V): String = encodeValue(value)
  }

  final class KeywordListMeta[V](underlying: ZSearchAttributeMeta.Of[V, String]) extends ZSearchAttributeMeta[List[V]] {

    override type Repr = ju.List[String]

    override def attributeKey(name: String): SearchAttributeKey[ju.List[String]] = {
      SearchAttributeKey.forKeywordList(name)
    }

    override def decode(values: ju.List[String]): List[V] = {
      values.asScala.map(value => underlying.decode(value)).toList
    }

    override def encode(values: List[V]): ju.List[String] = {
      values.map(value => underlying.encode(value)).asJava
    }
  }
}

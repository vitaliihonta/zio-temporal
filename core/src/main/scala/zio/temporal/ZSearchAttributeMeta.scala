package zio.temporal

import io.temporal.common.SearchAttributeKey

import java.time.{Instant, LocalDateTime, OffsetDateTime, ZoneOffset}
import java.util.UUID
import java.{util => ju}
import scala.annotation.implicitNotFound
import scala.jdk.CollectionConverters._

/** Encapsulates description & recipe for a Scala type stored as Temporal Search attribute.
  *
  * @tparam A
  *   Scala type
  */
@implicitNotFound(""""Type ${A} is not a valid type for a Temporal Search Attribute.
Either convert it to a supported type or provide an implicit ZSearchAttributeMeta instance
"""")
sealed trait ZSearchAttributeMeta[A] { self =>

  /** Representation supported by Temporal
    */
  type Repr

  /** Construct a typed [[SearchAttributeKey]]
    *
    * @param name
    *   the key name
    * @return
    *   search attribute key
    */
  def attributeKey(name: String): SearchAttributeKey[Repr]

  /** Encodes the attribute value to its representation type
    *
    * @param value
    *   Scala representation of the search attribute
    * @return
    *   representation of the value supported by Temporal
    */
  def encode(value: A): Repr

  /** Decodes the attribute value from its representation type
    *
    * @param value
    *   representation of the value supported by Temporal
    *
    * @return
    *   Scala representation of the search attribute
    */
  def decode(value: Repr): A

  /** Creates a new [[ZSearchAttributeMeta]] by converting this type to a new type.
    *
    * @tparam B
    *   the new search attribute type
    * @param project
    *   direct conversion [[A]] => [[B]]
    * @param reverse
    *   reverse conversion [[B]] => [[A]]
    * @return
    *   a new meta
    */
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

object ZSearchAttributeMeta extends ZSearchAttributeMetaCollectionInstances with ZSearchAttributeMetaEnumInstances {

  type Of[A, R] = ZSearchAttributeMeta[A] { type Repr = R }

  /** Summons an instance of [[ZSearchAttributeMeta]]
    *
    * @tparam A
    *   Scala type
    * @return
    *   summoned instance for the provided scala type
    */
  def apply[A](implicit meta: ZSearchAttributeMeta[A]): meta.type = meta

  /** Meta for a string type represented as Keyword type
    */
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

  implicit def optionInstance[V](
    implicit asString: ZSearchAttributeMeta.Of[V, String]
  ): ZSearchAttributeMeta.Of[Option[V], ju.List[String]] =
    keywordListImpl[V].convert[Option[V]](_.headOption, _.toList)

  protected[zio] def keywordListImpl[V](
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

package zio.temporal

import io.temporal.common.SearchAttributeKey
import zio.temporal.ZSearchAttribute.Plain
import zio.temporal.ZSearchAttribute.Keyword

import java.time.{Instant, LocalDateTime, OffsetDateTime, ZoneOffset}
import java.util.UUID
import java.{util => ju}
import scala.annotation.implicitNotFound
import scala.jdk.CollectionConverters._
import scala.reflect.ClassTag

/** Encapsulates description & recipe for a Scala type stored as Temporal Search attribute.
  *
  * @tparam A
  *   Scala type
  */
@implicitNotFound(""""Type ${A} is not a valid type for a Temporal Search Attribute.
Either convert it to a supported type or provide an implicit ZSearchAttributeMeta instance
""")
sealed trait ZSearchAttributeMeta[A, Tag] { self =>

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
  def convert[B](project: A => B)(reverse: B => A): ZSearchAttributeMeta.Of[B, Tag, self.Repr] =
    new ZSearchAttributeMeta[B, Tag] {
      override type Repr = self.Repr

      override def attributeKey(name: String): SearchAttributeKey[Repr] =
        self.attributeKey(name)

      override def encode(value: B): Repr =
        self.encode(reverse(value))

      override def decode(value: Repr): B =
        project(self.decode(value))
    }

  def downcastTag: ZSearchAttributeMeta.Of[A, Any, self.Repr] =
    self.asInstanceOf[ZSearchAttributeMeta.Of[A, Any, self.Repr]]
}

object ZSearchAttributeMeta extends ZSearchAttributeMetaCollectionInstances with ZSearchAttributeMetaEnumInstances {

  type Of[A, Tag, R] = ZSearchAttributeMeta[A, Tag] { type Repr = R }

  /** Summons an instance of [[ZSearchAttributeMeta]]
    *
    * @tparam A
    *   Scala type
    * @return
    *   summoned instance for the provided scala type
    */
  def apply[A, Tag](implicit meta: ZSearchAttributeMeta[A, Tag]): meta.type = meta

  /** Meta for a string type represented as Keyword type
    */
  implicit val stringKeyword: ZSearchAttributeMeta.Of[String, Keyword, String] =
    new KeywordMeta[String](identity, identity)

  implicit val string: ZSearchAttributeMeta.Of[String, Plain, String]    = StringMeta
  implicit val boolean: ZSearchAttributeMeta.Of[Boolean, Plain, Boolean] = BooleanMeta
  implicit val long: ZSearchAttributeMeta.Of[Long, Plain, Long]          = LongMeta
  implicit val integer: ZSearchAttributeMeta.Of[Int, Plain, Long]        = long.convert(_.toInt)(_.toLong)
  implicit val double: ZSearchAttributeMeta.Of[Double, Plain, Double]    = DoubleMeta

  implicit val bigInt: ZSearchAttributeMeta.Of[BigInt, Plain, String] =
    string.convert(BigInt(_))(_.toString)

  implicit val bigDecimal: ZSearchAttributeMeta.Of[BigDecimal, Plain, String] =
    string.convert(BigDecimal(_))(_.toString)

  implicit val uuid: ZSearchAttributeMeta.Of[UUID, Keyword, String] =
    stringKeyword.convert(UUID.fromString)(_.toString)

  implicit val offsetDateTime: ZSearchAttributeMeta.Of[OffsetDateTime, Plain, OffsetDateTime] =
    OffsetDateTimeMeta

  implicit val localDateTime: ZSearchAttributeMeta.Of[LocalDateTime, Plain, OffsetDateTime] =
    offsetDateTime.convert(_.toLocalDateTime)(_.atOffset(ZoneOffset.UTC))

  implicit val instant: ZSearchAttributeMeta.Of[Instant, Plain, OffsetDateTime] =
    offsetDateTime.convert(_.toInstant)(_.atOffset(ZoneOffset.UTC))

  implicit val currentTimeMillis: ZSearchAttributeMeta.Of[ZCurrentTimeMillis, Plain, OffsetDateTime] =
    offsetDateTime.convert(otd => new ZCurrentTimeMillis(otd.toInstant.toEpochMilli))(_.toOffsetDateTime())

  implicit def option[V, Tag, R](
    implicit underlying: ZSearchAttributeMeta.Of[V, Tag, R]
  ): ZSearchAttributeMeta.Of[Option[V], Tag, R] =
    new OptionMeta[V, Tag, R](underlying)

  implicit def array[V: ClassTag](
    implicit asString: ZSearchAttributeMeta.Of[V, Keyword, String]
  ): ZSearchAttributeMeta.Of[Array[V], Keyword, ju.List[String]] =
    keywordListImpl[V].convert(_.toArray)(_.toList)

  protected[zio] def keywordListImpl[V](
    implicit asString: ZSearchAttributeMeta.Of[V, Keyword, String]
  ): ZSearchAttributeMeta.Of[List[V], Keyword, ju.List[String]] =
    new KeywordListMeta[V](asString)

  private[zio] sealed trait SimplePlainMeta[A] extends ZSearchAttributeMeta[A, Plain] {
    override final type Repr = A

    protected def underlyingAttributeKey(name: String): SearchAttributeKey[A]

    override def attributeKey(name: String): SearchAttributeKey[A] = {
      underlyingAttributeKey(name)
    }

    override def encode(value: A): A = value

    override def decode(value: A): A = value
  }

  private final object StringMeta extends SimplePlainMeta[String] {
    override def underlyingAttributeKey(name: String): SearchAttributeKey[String] = {
      SearchAttributeKey.forText(name)
    }
  }

  private final object BooleanMeta extends SimplePlainMeta[Boolean] {
    override def underlyingAttributeKey(name: String): SearchAttributeKey[Boolean] = {
      // safe to cast java.lang.Boolean to Boolean
      SearchAttributeKey.forBoolean(name).asInstanceOf[SearchAttributeKey[Boolean]]
    }
  }

  private final object LongMeta extends SimplePlainMeta[Long] {
    override def underlyingAttributeKey(name: String): SearchAttributeKey[Long] = {
      // safe to cast java.lang.Long to Long
      SearchAttributeKey.forLong(name).asInstanceOf[SearchAttributeKey[Long]]
    }
  }

  private final object DoubleMeta extends SimplePlainMeta[Double] {
    override def underlyingAttributeKey(name: String): SearchAttributeKey[Double] = {
      // safe to cast java.lang.Double to Double
      SearchAttributeKey.forDouble(name).asInstanceOf[SearchAttributeKey[Double]]
    }
  }

  private final object OffsetDateTimeMeta extends SimplePlainMeta[OffsetDateTime] {
    override def underlyingAttributeKey(name: String): SearchAttributeKey[OffsetDateTime] = {
      SearchAttributeKey.forOffsetDateTime(name)
    }
  }

  private final class OptionMeta[A, Tag, R](underlying: ZSearchAttributeMeta.Of[A, Tag, R])
      extends ZSearchAttributeMeta[Option[A], Tag] {

    override type Repr = R

    override def attributeKey(name: String): SearchAttributeKey[R] = {
      // option isn't encoded as a value
      underlying.attributeKey(name)
    }

    override def encode(value: Option[A]): R = {
      value.map(underlying.encode).getOrElse {
        null.asInstanceOf[R]
      }
    }

    override def decode(value: R): Option[A] = {
      Option(value).map(underlying.decode)
    }
  }

  final class KeywordMeta[V](encodeValue: V => String, decodeValue: String => V)
      extends ZSearchAttributeMeta[V, Keyword] {
    override type Repr = String

    override def attributeKey(name: String): SearchAttributeKey[String] = {
      SearchAttributeKey.forKeyword(name)
    }

    override def decode(value: String): V = {
      decodeValue(value)
    }

    override def encode(value: V): String = {
      encodeValue(value)
    }
  }

  final class KeywordListMeta[V](val underlying: ZSearchAttributeMeta.Of[V, Keyword, String])
      extends ZSearchAttributeMeta[List[V], Keyword] {

    override type Repr = ju.List[String]

    override def attributeKey(name: String): SearchAttributeKey[ju.List[String]] = {
      // safe to cast as Plain is a tagged type
      SearchAttributeKey.forKeywordList(name)
    }

    override def decode(values: ju.List[String]): List[V] = {
      values.asScala
        .map(underlying.decode)
        .toList
    }

    override def encode(values: List[V]): ju.List[String] = {
      values.map(underlying.encode).asJava
    }
  }
}

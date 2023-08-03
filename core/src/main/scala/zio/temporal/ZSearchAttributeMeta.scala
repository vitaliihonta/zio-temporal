package zio.temporal

import io.temporal.common.SearchAttributeKey
import zio.temporal.ZSearchAttribute.Plain
import zio.temporal.ZSearchAttribute.Keyword
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
  implicit val stringKeyword: ZSearchAttributeMeta.Of[String, Keyword]   = new KeywordMeta[String](identity, identity)
  implicit val string: ZSearchAttributeMeta.Of[String, Plain[String]]    = StringMeta
  implicit val boolean: ZSearchAttributeMeta.Of[Boolean, Plain[Boolean]] = BooleanMeta
  implicit val long: ZSearchAttributeMeta.Of[Long, Plain[Long]]          = LongMeta
  implicit val integer: ZSearchAttributeMeta.Of[Int, Plain[Long]]        = long.convert(_.toInt, _.toLong)
  implicit val double: ZSearchAttributeMeta.Of[Double, Plain[Double]]    = DoubleMeta

  implicit val bigInt: ZSearchAttributeMeta.Of[BigInt, Plain[String]] =
    string.convert(BigInt(_), _.toString)

  implicit val bigDecimal: ZSearchAttributeMeta.Of[BigDecimal, Plain[String]] =
    string.convert(BigDecimal(_), _.toString)

  implicit val uuid: ZSearchAttributeMeta.Of[UUID, Keyword] =
    stringKeyword.convert(UUID.fromString, _.toString)

  implicit val offsetDateTime: ZSearchAttributeMeta.Of[OffsetDateTime, Plain[OffsetDateTime]] =
    OffsetDateTimeMeta

  implicit val localDateTime: ZSearchAttributeMeta.Of[LocalDateTime, Plain[OffsetDateTime]] =
    offsetDateTime.convert(_.toLocalDateTime, _.atOffset(ZoneOffset.UTC))

  implicit val instant: ZSearchAttributeMeta.Of[Instant, Plain[OffsetDateTime]] =
    offsetDateTime.convert(_.toInstant, _.atOffset(ZoneOffset.UTC))

  implicit val currentTimeMillis: ZSearchAttributeMeta.Of[ZCurrentTimeMillis, Plain[OffsetDateTime]] =
    offsetDateTime.convert(otd => new ZCurrentTimeMillis(otd.toInstant.toEpochMilli), _.toOffsetDateTime())

  implicit def plainOptionInstance[V, R](
    implicit underlying: ZSearchAttributeMeta.Of[V, Plain[R]]
  ): ZSearchAttributeMeta.Of[Option[V], Plain[R]] =
    new OptionMetaPlain[V, R](underlying)

  implicit def plainKeywordInstance[V, R](
    implicit underlying: ZSearchAttributeMeta.Of[V, Keyword]
  ): ZSearchAttributeMeta.Of[Option[V], Keyword] =
    new OptionMetaKeyword[V](underlying)

  protected[zio] def keywordListImpl[V](
    implicit asString: ZSearchAttributeMeta.Of[V, Keyword]
  ): ZSearchAttributeMeta.Of[List[V], Plain[ju.List[String]]] =
    new KeywordListMeta[V](asString)

  private[zio] sealed trait SimpleMeta[A] extends ZSearchAttributeMeta[A] {
    override final type Repr = Plain[A]

    protected def underlyingAttributeKey(name: String): SearchAttributeKey[A]

    override def attributeKey(name: String): SearchAttributeKey[Plain[A]] = {
      // safe to cast as Plain is a tagged type
      underlyingAttributeKey(name).asInstanceOf[SearchAttributeKey[Plain[A]]]
    }

    override def encode(value: A): Plain[A] = Plain(value)

    override def decode(value: Plain[A]): A = Plain.unwrap(value)
  }

  private final object StringMeta extends SimpleMeta[String] {
    override def underlyingAttributeKey(name: String): SearchAttributeKey[String] = {
      SearchAttributeKey.forText(name)
    }
  }

  private final object BooleanMeta extends SimpleMeta[Boolean] {
    override def underlyingAttributeKey(name: String): SearchAttributeKey[Boolean] = {
      // safe to cast java.lang.Boolean to Boolean
      SearchAttributeKey.forBoolean(name).asInstanceOf[SearchAttributeKey[Boolean]]
    }
  }

  private final object LongMeta extends SimpleMeta[Long] {
    override def underlyingAttributeKey(name: String): SearchAttributeKey[Long] = {
      // safe to cast java.lang.Long to Long
      SearchAttributeKey.forLong(name).asInstanceOf[SearchAttributeKey[Long]]
    }
  }

  private final object DoubleMeta extends SimpleMeta[Double] {
    override def underlyingAttributeKey(name: String): SearchAttributeKey[Double] = {
      // safe to cast java.lang.Double to Double
      SearchAttributeKey.forDouble(name).asInstanceOf[SearchAttributeKey[Double]]
    }
  }

  private final object OffsetDateTimeMeta extends SimpleMeta[OffsetDateTime] {
    override def underlyingAttributeKey(name: String): SearchAttributeKey[OffsetDateTime] = {
      SearchAttributeKey.forOffsetDateTime(name)
    }
  }

  private final class OptionMetaPlain[A, R](underlying: ZSearchAttributeMeta.Of[A, Plain[R]])
      extends ZSearchAttributeMeta[Option[A]] {

    override type Repr = Plain[R]

    override def attributeKey(name: String): SearchAttributeKey[Plain[R]] = {
      // option isn't encoded as a value
      underlying.attributeKey(name)
    }

    override def encode(value: Option[A]): Plain[R] = {
      value.map(underlying.encode).getOrElse {
        null.asInstanceOf[Plain[R]]
      }
    }

    override def decode(value: Plain[R]): Option[A] = {
      Option(value).map(underlying.decode)
    }
  }

  private final class OptionMetaKeyword[A](underlying: ZSearchAttributeMeta.Of[A, Keyword])
      extends ZSearchAttributeMeta[Option[A]] {

    override type Repr = Keyword

    override def attributeKey(name: String): SearchAttributeKey[Keyword] = {
      // option isn't encoded as a value
      underlying.attributeKey(name)
    }

    override def encode(value: Option[A]): Keyword = {
      value.map(underlying.encode).getOrElse {
        null.asInstanceOf[Keyword]
      }
    }

    override def decode(value: Keyword): Option[A] = {
      Option(value).map(underlying.decode)
    }
  }

  final class KeywordMeta[V](encodeValue: V => String, decodeValue: String => V) extends ZSearchAttributeMeta[V] {
    override type Repr = Keyword

    override def attributeKey(name: String): SearchAttributeKey[Keyword] = {
      // safe to cast as Keyword is just a tagged type
      SearchAttributeKey
        .forKeyword(name)
        .asInstanceOf[SearchAttributeKey[Keyword]]
    }

    override def decode(value: Keyword): V = {
      decodeValue(value)
    }

    override def encode(value: V): Keyword = {
      Keyword(encodeValue(value))
    }
  }

  final class KeywordListMeta[V](val underlying: ZSearchAttributeMeta.Of[V, Keyword])
      extends ZSearchAttributeMeta[List[V]] {

    override type Repr = Plain[ju.List[String]]

    override def attributeKey(name: String): SearchAttributeKey[Plain[ju.List[String]]] = {
      // safe to cast as Plain is a tagged type
      SearchAttributeKey.forKeywordList(name).asInstanceOf[SearchAttributeKey[Plain[ju.List[String]]]]
    }

    override def decode(values: Plain[ju.List[String]]): List[V] = {
      Plain
        .unwrap(values)
        .asScala
        .map(value => underlying.decode(Keyword(value)))
        .toList
    }

    override def encode(values: List[V]): Plain[ju.List[String]] = {
      val result = values.map { (value: V) =>
        val encoded: Keyword = underlying.encode(value)
        Keyword.unwrap(encoded)
      }.asJava

      Plain(result)
    }
  }
}

package zio.temporal

import io.temporal.common.{SearchAttributeKey, SearchAttributeUpdate, SearchAttributes}
import java.time.{Instant, LocalDateTime, OffsetDateTime, ZoneOffset}
import java.util.UUID
import java.{util => ju}
import scala.annotation.implicitNotFound
import scala.jdk.CollectionConverters._
import scala.language.implicitConversions

/** Base type for attribute value.
  *
  * Restricted to allowed elasticsearch types.
  * @see
  *   https://docs.temporal.io/docs/server/workflow-search#search-attributes
  */
sealed trait ZSearchAttribute {
  type ValueType

  def value: ValueType

  def meta: ZSearchAttributeMeta[ValueType]

  override def toString: String = {
    s"ZSearchAttribute(" +
      s"value=$value" +
      s", meta=$meta" +
      s")"
  }
}

object ZSearchAttribute {
  type Of[A] = ZSearchAttribute { type ValueType = A }

  /** Explicitly sets attribute type to Keyword
    *
    * @param value
    *   search attribute value
    */
  def keyword(value: String): ZSearchAttribute.Of[String] =
    new SearchAttributeImpl[String](value, ZSearchAttributeMeta.stringKeyword)

  /** Converts a value to [[ZSearchAttribute]] having implicit [[ZSearchAttributeMeta]] instance
    *
    * @param value
    *   attributes value to convert
    * @param meta
    *   conversion typeclass
    * @return
    *   converted search attribute
    */
  implicit def from[A](value: A)(implicit meta: ZSearchAttributeMeta[A]): ZSearchAttribute.Of[A] =
    new SearchAttributeImpl[A](value, meta)

  /** Converts custom search attributes to [[java.util.Map]] that temporal Java SDK can consume
    *
    * @param attrs
    *   attributes to convert
    * @return
    *   attributes converted
    */
  implicit def toJava(attrs: Map[String, ZSearchAttribute]): ju.Map[String, AnyRef] =
    attrs
      .map { case (k, attr) =>
        val meta = attr.meta
        k -> meta.encode(attr.value)
      }
      .asInstanceOf[Map[String, AnyRef]]
      .asJava

  /** Converts custom search attributes to a list of [[SearchAttributeUpdate]] that temporal Java SDK can consume
    *
    * @param attrs
    *   attributes to convert
    * @return
    *   attributes converted
    */
  def toJavaAttributeUpdates(attrs: Map[String, ZSearchAttribute]): List[SearchAttributeUpdate[_]] =
    attrs.map { case (name, attr) =>
      val meta = attr.meta
      SearchAttributeUpdate.valueSet[meta.Repr](
        meta.attributeKey(name),
        meta.encode(attr.value)
      )
    }.toList

  /** Converts custom search attributes to a list of [[SearchAttributes]] that temporal Java SDK can consume
    *
    * @param attrs
    *   attributes to convert
    * @return
    *   attributes converted
    */
  def toJavaSearchAttributes(attrs: Map[String, ZSearchAttribute]): SearchAttributes = {
    attrs
      .foldLeft(SearchAttributes.newBuilder()) { case (builder, (name, attr)) =>
        val meta = attr.meta
        builder.set[meta.Repr](
          meta.attributeKey(name),
          meta.encode(attr.value)
        )
      }
      .build()
  }

  private[zio] final class SearchAttributeImpl[V](
    override val value: V,
    override val meta:  ZSearchAttributeMeta[V])
      extends ZSearchAttribute {

    override type ValueType = V
  }
}

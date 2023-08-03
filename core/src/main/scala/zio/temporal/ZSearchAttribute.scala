package zio.temporal

import io.temporal.common.{SearchAttributeUpdate, SearchAttributes}
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

  def meta: ZSearchAttributeMeta[ValueType, Any]

  override def toString: String = {
    s"ZSearchAttribute(" +
      s"value=$value" +
      s", meta=$meta" +
      s")"
  }
}

object ZSearchAttribute {
  type Of[A] = ZSearchAttribute { type ValueType = A }

  sealed trait Tag

  /** Used to indicate that a type is encoded as a plain attribute
    */
  sealed trait Plain extends Tag

  /** Used to indicate that a type is encoded as Keyword attribute
    */
  sealed trait Keyword extends Tag

  /** Converts a value to [[ZSearchAttribute]] having implicit [[ZSearchAttributeMeta]] instance
    *
    * @param value
    *   attributes value to convert
    * @param meta
    *   conversion typeclass
    * @return
    *   converted search attribute
    */
  def apply[A](value: A)(implicit meta: ZSearchAttributeMeta[A, Plain]): ZSearchAttribute.Of[A] =
    new SearchAttributeImpl[A](value, meta.downcastTag)

  /** Explicitly sets attribute type to Keyword
    *
    * @param value
    *   search attribute value
    */
  def keyword[A](value: A)(implicit meta: ZSearchAttributeMeta[A, Keyword]): ZSearchAttribute.Of[A] =
    new SearchAttributeImpl[A](value, meta.downcastTag)

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
    override val meta:  ZSearchAttributeMeta[V, Any])
      extends ZSearchAttribute {

    override type ValueType = V
  }
}

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

  /** Used to indicate that a type is encoded as a plain attribute
    */
  sealed trait PlainTag
  final type Plain[+A] = A with PlainTag
  final object Plain {
    def apply[A](value: A): Plain[A] = value.asInstanceOf[Plain[A]]

    def unwrap[A](plain: Plain[A]): A = plain
  }

  /** Used to indicate that a type is encoded as Keyword attribute
    */
  sealed trait KeywordTag

  final type Keyword = String with KeywordTag
  final object Keyword {

    def apply(value: String): Keyword = value.asInstanceOf[Keyword]

    def unwrap(keyword: Keyword): String = keyword
  }

  /** Converts a value to [[ZSearchAttribute]] having implicit [[ZSearchAttributeMeta]] instance
    *
    * @param value
    *   attributes value to convert
    * @param meta
    *   conversion typeclass
    * @return
    *   converted search attribute
    */
  def apply[A, R](value: A)(implicit meta: ZSearchAttributeMeta.Of[A, Plain[R]]): ZSearchAttribute.Of[A] =
    new SearchAttributeImpl[A](value, meta)

  /** Explicitly sets attribute type to Keyword
    *
    * @param value
    *   search attribute value
    */
  def keyword[A](value: A)(implicit meta: ZSearchAttributeMeta.Of[A, Keyword]): ZSearchAttribute.Of[A] =
    new SearchAttributeImpl[A](value, meta)

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

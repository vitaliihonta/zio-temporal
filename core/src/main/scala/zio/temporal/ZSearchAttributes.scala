package zio.temporal

import io.temporal.common.{SearchAttributeKey, SearchAttributes}
import scala.jdk.CollectionConverters._
import java.{util => ju}

/** Immutable collection of typed search attributes.
  */
final class ZSearchAttributes private[zio] (val toJava: SearchAttributes) {

  /** Get whether the search attribute key exists. */
  def containsKey[T](name: String)(implicit meta: ZSearchAttributeMeta[T]): Boolean =
    toJava.containsKey(meta.attributeKey(name))

  /** Get a search attribute value by its key or null if not present.
    *
    * @throws ClassCastException
    *   If the search attribute is not of the proper type for the key.
    */
  def get[T](name: String)(implicit meta: ZSearchAttributeMeta[T]): Option[T] = Option(
    toJava.get[meta.Repr](meta.attributeKey(name))
  ).map(meta.decode)

  /** Get the size of the collection. */
  def size: Int =
    toJava.size()

  /** Get the immutable, untyped values map.
    * @note
    *   values are encoded (e.g., UUID is represented as String). Java collections are converted to Scala collections
    */
  def untypedValues: Map[SearchAttributeKey[_], AnyRef] =
    toJava.getUntypedValues.asScala.map {
      case (key, value: ju.Collection[_]) =>
        key -> value.asScala.toList

      case (key, value) =>
        key -> value
    }.toMap

  override def toString: String = untypedValues
    .map { case (key, value) =>
      s"${attrKeyAsString(key)} -> $value}"
    }
    .mkString("ZSearchAttributes(", ", ", ")")

  private def attrKeyAsString(key: SearchAttributeKey[_]) =
    s"SearchAttributeKey(" +
      s"name=${key.getName}" +
      s", valueType=${key.getValueType}" +
      s", valueClass=${key.getValueClass}" +
      s", valueReflectType=${key.getValueReflectType}" +
      s")"
}

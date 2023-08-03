package zio.temporal

import zio.temporal.internal.Scala3EnumUtils
import scala.quoted._

trait ZSearchAttributeMetaEnumInstances {

  /** Provides an attribute meta for Scala 3 native enum types.
    */
  inline given enumAttribute[E <: scala.reflect.Enum]: ZSearchAttributeMeta.Of[E, String] = {
    val enumMeta = Scala3EnumUtils.getEnumMeta[E]
    new ZSearchAttributeMeta.KeywordMeta[E](_.toString, enumMeta.valueOf)
  }
}

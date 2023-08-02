package zio.temporal

import zio.temporal.internal.Scala3EnumUtils
import scala.quoted._

trait ZSearchAttributeMetaVersionSpecific {

  /** Provides automatic instance derivation for Scala 3 enum types.
    */
  inline given enumAttribute[E <: scala.reflect.Enum]: ZSearchAttributeMeta.Of[E, String] = {
    val enumMeta = Scala3EnumUtils.getEnumMeta[E]
    new ZSearchAttributeMeta.KeywordMeta[E](_.toString, enumMeta.valueOf)
  }
}

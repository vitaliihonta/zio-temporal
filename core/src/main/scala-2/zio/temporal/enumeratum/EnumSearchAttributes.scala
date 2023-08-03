package zio.temporal.enumeratum

import _root_.enumeratum._
import _root_.enumeratum.values._
import zio.temporal.{ZSearchAttribute, ZSearchAttributeMeta}

/** Provides automatic instance derivation for enumeratum types. It's an optional dependency which won't be added to the
  * classpath unless you use it
  */
trait EnumSearchAttributes {

  implicit def enumAttribute[E <: EnumEntry](
    implicit enum: Enum[E]
  ): ZSearchAttributeMeta.Of[E, ZSearchAttribute.Keyword, String] =
    new ZSearchAttributeMeta.KeywordMeta[E](_.entryName, enum.withName)

  implicit def stringEnumAttribute[E <: StringEnumEntry](
    implicit enum: StringEnum[E]
  ): ZSearchAttributeMeta.Of[E, ZSearchAttribute.Keyword, String] =
    new ZSearchAttributeMeta.KeywordMeta[E](_.value, enum.withValue)
}

package zio.temporal.enumeratum

import _root_.enumeratum.*
import _root_.enumeratum.values.*
import zio.temporal.{VersionSpecificConverters, ZSearchAttribute}

/** Provides automatic instance derivation for enumeratum types. It's an optional dependency which won't be added to the
  * classpath unless you use it
  */
trait EnumSearchAttributes {

  implicit def enumAttribute[E <: EnumEntry]: ZSearchAttribute.Convert[E] =
    ZSearchAttribute.Convert.string.contramap(_.entryName)

  implicit def stringEnumAttribute[E <: StringEnumEntry]: ZSearchAttribute.Convert[E] =
    ZSearchAttribute.Convert.string.contramap(_.value)
}

object EnumSearchAttributes extends VersionSpecificConverters

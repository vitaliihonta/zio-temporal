package ztemporal.enumeratum

import _root_.enumeratum._
import _root_.enumeratum.values._
import ztemporal.ZSearchAttribute

/** Provides automatic instance derivation for enumeratum types.
  * It's an optional dependency which won't be added to the classpath unless you use it
  */
trait EnumeratumSearchAttributes {

  implicit def enumAttribute[E <: EnumEntry]: ZSearchAttribute.Convert[E] =
    ZSearchAttribute.Convert.string.contramap(_.entryName)

  implicit def stringEnumAttribute[E <: StringEnumEntry]: ZSearchAttribute.Convert[E] =
    ZSearchAttribute.Convert.string.contramap(_.value)
}

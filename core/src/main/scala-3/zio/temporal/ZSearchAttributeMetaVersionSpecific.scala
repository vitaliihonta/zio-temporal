package zio.temporal

import scala.quoted._
import scala.deriving.Mirror

trait ZSearchAttributeMetaVersionSpecific {
  // TODO: implement
  given enumAttribute[E <: scala.reflect.Enum]: ZSearchAttributeMeta.Of[E, String] =
    ???
}

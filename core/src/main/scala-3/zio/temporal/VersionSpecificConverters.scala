package zio.temporal

import scala.quoted._
import scala.deriving.Mirror

trait VersionSpecificConverters {
  given enumAttribute[E <: scala.reflect.Enum]: ZSearchAttribute.Convert[E] =
    ZSearchAttribute.Convert.string.contramap(_.toString)
}

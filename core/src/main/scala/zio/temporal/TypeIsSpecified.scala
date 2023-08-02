package zio.temporal

import scala.annotation.implicitAmbiguous

sealed abstract class TypeIsSpecified[-E]

object TypeIsSpecified extends TypeIsSpecified[Any] {

  implicit def typeIsSpecified[E]: TypeIsSpecified[E] = TypeIsSpecified

  // Provide multiple ambiguous values so an implicit TypeIsSpecified[Nothing] cannot be found.
  @implicitAmbiguous(
    "Type parameter must be specified explicitly"
  )
  implicit val TypeIsSpecifiedAmbiguous1: TypeIsSpecified[Nothing] = TypeIsSpecified
  implicit val TypeIsSpecifiedAmbiguous2: TypeIsSpecified[Nothing] = TypeIsSpecified
}

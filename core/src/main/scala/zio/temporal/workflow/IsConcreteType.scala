package zio.temporal.workflow

import scala.annotation.implicitAmbiguous

/** A value of type `IsConcreteType[E]` provides implicit evidence that a proxy type `A` is concrete
  */
sealed abstract class IsConcreteType[-A]

object IsConcreteType extends IsConcreteType[Any] {

  implicit def isConcreteType[A]: IsConcreteType[A] = IsConcreteType

  // Provide multiple ambiguous values so an implicit CanFail[Nothing] cannot be found.
  @implicitAmbiguous(
    "This method requires you to provide a workflow interface type.\n" +
      "You should pass it as a concrete type parameter. In this case, it was inferred as scala.Nothing"
  )
  implicit val isConcreteTypeAmbiguous1: IsConcreteType[Nothing] = IsConcreteType
  implicit val isConcreteTypeAmbiguous2: IsConcreteType[Nothing] = IsConcreteType
}

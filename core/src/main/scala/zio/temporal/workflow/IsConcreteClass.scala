package zio.temporal.workflow

import zio.temporal.internalApi

trait IsConcreteClass[A] {}

object IsConcreteClass extends IsConcreteClassImplicits {
  def apply[A](implicit ev: IsConcreteClass[A]): ev.type = ev

  @internalApi
  final object __zio_temporal_IsConcreteClassInstance extends IsConcreteClass[Any]
}

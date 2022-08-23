package zio.temporal.workflow

import zio.temporal.internalApi

trait HasPublicNullaryConstructor[A] {}

object HasPublicNullaryConstructor extends HasPublicNullaryConstructorImplicits {
  def apply[A](implicit ev: HasPublicNullaryConstructor[A]): ev.type = ev

  @internalApi
  final object __zio_temporal_HasPublicNullaryConstructorInstance extends HasPublicNullaryConstructor[Any]
}

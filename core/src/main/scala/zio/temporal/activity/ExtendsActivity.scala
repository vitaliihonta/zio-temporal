package zio.temporal.activity

import zio.temporal.internalApi

trait ExtendsActivity[A] {}

object ExtendsActivity extends ExtendsActivityImplicits {
  def apply[A](implicit ev: ExtendsActivity[A]): ev.type = ev

  @internalApi
  final object __zio_temporal_ExtendsActivityInstance extends ExtendsActivity[Any]
}

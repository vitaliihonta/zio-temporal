package zio.temporal.activity

import zio.temporal.internalApi

trait IsActivity[A] {}

object IsActivity extends IsActivityImplicits {
  def apply[A](implicit ev: IsActivity[A]): ev.type = ev

  @internalApi
  final object __zio_temporal_IsActivityInstance extends IsActivity[Any]
}

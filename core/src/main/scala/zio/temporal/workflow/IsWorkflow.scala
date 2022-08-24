package zio.temporal.workflow

import zio.temporal.internalApi

trait IsWorkflow[A] {}

object IsWorkflow extends IsWorkflowImplicits {
  def apply[A](implicit ev: IsWorkflow[A]): ev.type = ev

  @internalApi
  final object __zio_temporal_IsWorkflowInstance extends IsWorkflow[Any]
}

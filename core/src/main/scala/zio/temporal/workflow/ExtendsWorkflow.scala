package zio.temporal.workflow

import zio.temporal.internalApi

// TODO: add for scala 3
trait ExtendsWorkflow[A] {}

object ExtendsWorkflow extends ExtendsWorkflowImplicits {
  def apply[A](implicit ev: ExtendsWorkflow[A]): ev.type = ev

  @internalApi
  final object __zio_temporal_ExtendsWorkflowInstance extends ExtendsWorkflow[Any]
}

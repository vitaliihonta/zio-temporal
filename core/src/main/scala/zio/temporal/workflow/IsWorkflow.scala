package zio.temporal.workflow

import zio.temporal.internalApi

trait IsWorkflow[A] {}

object IsWorkflow extends IsWorkflowImplicits {
  def apply[A](implicit ev: IsWorkflow[A]): ev.type = ev

  @internalApi
  final object __zio_temporal_IsWorkflowInstance extends IsWorkflow[Any]
}

trait IsWorkflowInterface[A] {
  def workflowType: String
}

object IsWorkflowInterface extends IsWorkflowInterfaceImplicits {
  def apply[A](implicit ev: IsWorkflowInterface[A]): ev.type = ev

  @internalApi
  final class __zio_temporal_IsWorkflowInterfaceInstance(val workflowType: String) extends IsWorkflowInterface[Any]
}

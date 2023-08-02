package zio.temporal.schedules

import io.temporal.common.interceptors.Header
import io.temporal.client.WorkflowOptions
import zio.temporal.internal.{BasicStubOps, Stubs}

/** Typed schedule start workflow stub that is used to start a scheduled workflow. Allows scheduling a workflow
  * type-safely by invoking the workflow method
  */
sealed trait ZScheduleStartWorkflowStub extends BasicStubOps {

  def workflowOptions: WorkflowOptions

  def header: Header
}

final class ZScheduleStartWorkflowStubImpl private[zio] (
  val stubbedClass:    Class[_],
  val workflowOptions: WorkflowOptions,
  val header:          Header)
    extends ZScheduleStartWorkflowStub

object ZScheduleStartWorkflowStub extends Stubs[ZScheduleStartWorkflowStub] with ZScheduleStartWorkflowStubSyntax {

  /** Untyped version of [[ZScheduleStartWorkflowStub]] */
  sealed trait Untyped {
    def workflowType: String

    def workflowOptions: WorkflowOptions

    def header: Header
  }

  final class UntypedImpl private[zio] (
    val workflowType:    String,
    val workflowOptions: WorkflowOptions,
    val header:          Header)
      extends Untyped {}

  final implicit class Ops[A](private val self: ZScheduleStartWorkflowStub.Of[A]) extends AnyVal {}

}

package zio.temporal.schedules

import io.temporal.common.interceptors.Header
import io.temporal.client.WorkflowOptions
import zio.temporal.internal.{BasicStubOps, Stubs}
import zio.temporal.internalApi
import scala.reflect.ClassTag

// todo: document
sealed trait ZScheduleStartWorkflowStub extends BasicStubOps {
  protected[zio] def workflowOptions: WorkflowOptions

  protected[zio] def header: Header
}

final class ZScheduleStartWorkflowStubImpl @internalApi() (
  override val stubbedClass:                   Class[_],
  override protected[zio] val workflowOptions: WorkflowOptions,
  override protected[zio] val header:          Header)
    extends ZScheduleStartWorkflowStub {}

object ZScheduleStartWorkflowStub extends Stubs[ZScheduleStartWorkflowStub] with ZScheduleStartWorkflowStubSyntax {
  sealed trait Untyped {
    def workflowType: String

    protected[zio] def workflowOptions: WorkflowOptions

    protected[zio] def header: Header
  }

  private[temporal] final class UntypedImpl @internalApi() (
    override val workflowType:                   String,
    override protected[zio] val workflowOptions: WorkflowOptions,
    override protected[zio] val header:          Header)
      extends Untyped {}

  final implicit class Ops[A](private val self: ZScheduleStartWorkflowStub.Of[A]) extends AnyVal {}

}

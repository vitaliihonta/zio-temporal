package zio.temporal.schedules

import io.temporal.common.interceptors.Header
import io.temporal.client.WorkflowOptions
import zio.temporal.internal.{BasicStubOps, Stubs}
import zio.temporal.internalApi
import scala.reflect.ClassTag

// todo: document
sealed trait ZScheduleStartWorkflowStub extends BasicStubOps {

  def workflowOptions: WorkflowOptions

  def header: Header
}

final class ZScheduleStartWorkflowStubImpl @internalApi() (
  val stubbedClass:    Class[_],
  val workflowOptions: WorkflowOptions,
  val header:          Header)
    extends ZScheduleStartWorkflowStub

object ZScheduleStartWorkflowStub extends Stubs[ZScheduleStartWorkflowStub] with ZScheduleStartWorkflowStubSyntax {
  sealed trait Untyped {
    def workflowType: String

    def workflowOptions: WorkflowOptions

    def header: Header
  }

  private[temporal] final class UntypedImpl @internalApi() (
    val workflowType:    String,
    val workflowOptions: WorkflowOptions,
    val header:          Header)
      extends Untyped {}

  final implicit class Ops[A](private val self: ZScheduleStartWorkflowStub.Of[A]) extends AnyVal {}

}

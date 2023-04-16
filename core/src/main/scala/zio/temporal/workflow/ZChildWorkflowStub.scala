package zio.temporal.workflow

import io.temporal.workflow.ChildWorkflowStub
import zio.temporal.internal.tagging.Stubs
import zio.temporal.{ZWorkflowExecution, internalApi}
import zio.temporal.query.ZWorkflowStubQuerySyntax
import zio.temporal.signal.ZChildWorkflowStubSignalSyntax

/** Represents untyped child workflow stub
  *
  * @see
  *   [[ChildWorkflowStub]]
  */
sealed trait ZChildWorkflowStub {
  def toJava: ChildWorkflowStub

  /** If workflow completes before this promise is ready then the child might not start at all.
    *
    * @return
    *   [[ZAsync]] that becomes ready once the child has started.
    */
  def getExecution: ZAsync[ZWorkflowExecution] =
    ZAsync
      .fromJava(toJava.getExecution)
      .map(new ZWorkflowExecution(_))
}

final class ZChildWorkflowStubImpl @internalApi() (val toJava: ChildWorkflowStub) extends ZChildWorkflowStub

object ZChildWorkflowStub
    extends Stubs[ZChildWorkflowStub]
    with ZChildWorkflowExecutionSyntax
    with ZWorkflowStubQuerySyntax
    with ZChildWorkflowStubSignalSyntax {

  final implicit class Ops[A](private val self: ZChildWorkflowStub.Of[A]) extends AnyVal {}
}

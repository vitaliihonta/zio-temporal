package zio.temporal.workflow

import io.temporal.workflow.ChildWorkflowStub
import zio.temporal.internal.CanSignal
import zio.temporal.internal.tagging.Stubs
import zio.temporal.internalApi
import zio.temporal.query.ZWorkflowStubQuerySyntax
import zio.temporal.signal.ZWorkflowStubSignalSyntax

/** Represents untyped child workflow stub
  *
  * @see
  *   [[ChildWorkflowStub]]
  */
sealed trait ZChildWorkflowStub extends CanSignal[ChildWorkflowStub] {

  override protected[zio] def signalMethod(signalName: String, args: Seq[AnyRef]): Unit =
    toJava.signal(signalName, args: _*)
}

final class ZChildWorkflowStubImpl @internalApi() (val toJava: ChildWorkflowStub) extends ZChildWorkflowStub

object ZChildWorkflowStub
    extends Stubs[ZChildWorkflowStub]
    with ZChildWorkflowExecutionSyntax
    with ZWorkflowStubSignalSyntax
    with ZWorkflowStubQuerySyntax {

  final implicit class Ops[A](private val self: ZChildWorkflowStub.Of[A]) extends AnyVal {

    /** Converts typed stub [[A]] to [[ZChildWorkflowStub]]
      *
      * @return
      *   untyped child workflow stub
      */
    def toStub: ZChildWorkflowStub = new ZChildWorkflowStubImpl(ChildWorkflowStub.fromTyped(self))
  }
}

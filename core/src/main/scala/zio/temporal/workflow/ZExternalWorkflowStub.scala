package zio.temporal.workflow

import io.temporal.workflow.ExternalWorkflowStub
import zio.temporal.internal.CanSignal
import zio.temporal.internal.tagging.Stubs
import zio.temporal.internalApi
import zio.temporal.query.ZWorkflowStubQuerySyntax
import zio.temporal.signal.ZWorkflowStubSignalSyntax

/** Represents untyped external workflow stub
  *
  * TODO: not sure whether it's needed
  * @see
  *   [[ExternalWorkflowStub]]
  */
sealed trait ZExternalWorkflowStub extends CanSignal[ExternalWorkflowStub] {

  override protected[zio] def signalMethod(signalName: String, args: Seq[AnyRef]): Unit =
    toJava.signal(signalName, args: _*)
}

final class ZExternalWorkflowStubImpl @internalApi() (val toJava: ExternalWorkflowStub) extends ZExternalWorkflowStub {}

object ZExternalWorkflowStub
    extends Stubs[ZExternalWorkflowStub]
    with ZExternalWorkflowExecutionSyntax
    with ZWorkflowStubSignalSyntax
    with ZWorkflowStubQuerySyntax {

  final implicit class Ops[A](private val self: ZExternalWorkflowStub.Of[A]) extends AnyVal {

    /** Converts typed stub [[A]] to [[ZExternalWorkflowStub]]
      *
      * @return
      *   untyped external workflow stub
      */
    def toStub: ZExternalWorkflowStub = new ZExternalWorkflowStubImpl(ExternalWorkflowStub.fromTyped(self))
  }
}

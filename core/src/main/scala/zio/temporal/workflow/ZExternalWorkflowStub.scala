package zio.temporal.workflow

import io.temporal.workflow.ExternalWorkflowStub
import zio.temporal.internal.CanSignal
import zio.temporal.internal.tagging.Stubs
import zio.temporal.internalApi
import zio.temporal.query.ZWorkflowStubQuerySyntax
import zio.temporal.signal.ZWorkflowStubSignalSyntax

/** Represents untyped external workflow stub
  *
  * @see
  *   [[ExternalWorkflowStub]]
  */
sealed trait ZExternalWorkflowStub extends CanSignal[ExternalWorkflowStub] {

  override protected[zio] def signalMethod(signalName: String, args: Seq[AnyRef]): Unit =
    toJava.signal(signalName, args: _*)
}

final class ZExternalWorkflowStubImpl @internalApi() (val toJava: ExternalWorkflowStub) extends ZExternalWorkflowStub {}

// TODO: add signal (non-IO, see https://docs.temporal.io/application-development/features?lang=java#send-signal-from-workflow)
object ZExternalWorkflowStub extends Stubs[ZExternalWorkflowStub] with ZExternalWorkflowExecutionSyntax {

  final implicit class Ops[A](private val self: ZExternalWorkflowStub.Of[A]) extends AnyVal {

    /** Converts typed stub [[A]] to [[ZExternalWorkflowStub]]
      *
      * @return
      *   untyped external workflow stub
      */
    def toStub: ZExternalWorkflowStub = new ZExternalWorkflowStubImpl(ExternalWorkflowStub.fromTyped(self))
  }
}

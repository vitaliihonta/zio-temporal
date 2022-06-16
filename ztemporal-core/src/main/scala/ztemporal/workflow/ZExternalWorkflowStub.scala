package ztemporal.workflow

import io.temporal.workflow.ExternalWorkflowStub
import ztemporal.internal.CanSignal
import ztemporal.internal.tagging.Tagged

/** Represents untyped external workflow stub
  *
  * @see
  *   [[ExternalWorkflowStub]]
  */
class ZExternalWorkflowStub private[ztemporal] (override protected[ztemporal] val self: ExternalWorkflowStub)
    extends AnyVal
    with CanSignal[ExternalWorkflowStub] {

  override protected[ztemporal] def signalMethod(signalName: String, args: Seq[AnyRef]): Unit =
    self.signal(signalName, args: _*)
}

object ZExternalWorkflowStub extends Tagged {

  final implicit class Ops[A](private val self: ZExternalWorkflowStub.Of[A]) extends AnyVal {

    /** Converts typed stub [[A]] to [[ZExternalWorkflowStub]]
      *
      * @return
      *   untyped external workflow stub
      */
    def toStub: ZExternalWorkflowStub = new ZExternalWorkflowStub(ExternalWorkflowStub.fromTyped(self))
  }
}

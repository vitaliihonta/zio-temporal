package ztemporal.workflow

import io.temporal.workflow.ChildWorkflowStub
import ztemporal.internal.CanSignal
import ztemporal.internal.tagging.Tagged

/** Represents untyped child workflow stub
  *
  * @see
  *   [[ChildWorkflowStub]]
  */
class ZChildWorkflowStub private[ztemporal] (override protected[ztemporal] val self: ChildWorkflowStub)
    extends AnyVal
    with CanSignal[ChildWorkflowStub] {

  override protected[ztemporal] def signalMethod(signalName: String, args: Seq[AnyRef]): Unit =
    self.signal(signalName, args: _*)
}

object ZChildWorkflowStub extends Tagged {

  final implicit class Ops[A](private val self: ZChildWorkflowStub.Of[A]) extends AnyVal {

    /** Converts typed stub [[A]] to [[ZChildWorkflowStub]]
      *
      * @return
      *   untyped child workflow stub
      */
    def toStub: ZChildWorkflowStub = new ZChildWorkflowStub(ChildWorkflowStub.fromTyped(self))
  }
}

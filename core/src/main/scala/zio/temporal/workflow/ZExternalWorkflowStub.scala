package zio.temporal.workflow

import io.temporal.workflow.ExternalWorkflowStub
import zio.temporal.internal.Stubs
import zio.temporal.internalApi
import zio.temporal.signal.ZExternalWorkflowStubSignalSyntax

/** Represents untyped external workflow stub
  *
  * @see
  *   [[ExternalWorkflowStub]]
  */
sealed trait ZExternalWorkflowStub {
  def toJava: ExternalWorkflowStub

  def untyped: ZExternalWorkflowStub.Untyped

  def cancel(): Unit =
    untyped.cancel()
}

final class ZExternalWorkflowStubImpl @internalApi() (val toJava: ExternalWorkflowStub) extends ZExternalWorkflowStub {
  override val untyped: ZExternalWorkflowStub.Untyped = new ZExternalWorkflowStub.UntypedImpl(toJava)
}

object ZExternalWorkflowStub extends Stubs[ZExternalWorkflowStub] with ZExternalWorkflowStubSignalSyntax {

  /** An untyped version of [[ZExternalWorkflowStub]]
    */
  sealed trait Untyped {
    def toJava: ExternalWorkflowStub

    def cancel(): Unit

    def signal(signalName: String, args: Any*): Unit
  }

  private[temporal] class UntypedImpl(val toJava: ExternalWorkflowStub) extends Untyped {
    override def cancel(): Unit =
      toJava.cancel()

    override def signal(signalName: String, args: Any*): Unit =
      toJava.signal(signalName, args.asInstanceOf[Seq[AnyRef]]: _*)
  }

  final implicit class Ops[A](private val self: ZExternalWorkflowStub.Of[A]) extends AnyVal {}
}

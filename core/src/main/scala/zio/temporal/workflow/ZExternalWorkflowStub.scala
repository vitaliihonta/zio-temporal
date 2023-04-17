package zio.temporal.workflow

import io.temporal.workflow.ExternalWorkflowStub
import zio.temporal.internal.TemporalInteraction
import zio.temporal.internal.tagging.Stubs
import zio.temporal.{TemporalIO, internalApi}
import zio.temporal.signal.ZExternalWorkflowStubSignalSyntax

/** Represents untyped external workflow stub
  *
  * @see
  *   [[ExternalWorkflowStub]]
  */
sealed trait ZExternalWorkflowStub {
  def toJava: ExternalWorkflowStub

  def cancel(): TemporalIO[Unit] =
    TemporalInteraction.from {
      toJava.cancel()
    }
}

final class ZExternalWorkflowStubImpl @internalApi() (val toJava: ExternalWorkflowStub) extends ZExternalWorkflowStub {}

object ZExternalWorkflowStub extends Stubs[ZExternalWorkflowStub] with ZExternalWorkflowStubSignalSyntax {

  final implicit class Ops[A](private val self: ZExternalWorkflowStub.Of[A]) extends AnyVal {}
}

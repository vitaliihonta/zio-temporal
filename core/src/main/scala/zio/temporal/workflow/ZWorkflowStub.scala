package zio.temporal.workflow

import io.temporal.client.WorkflowStub
import zio.temporal.{TemporalIO, internalApi}
import zio.temporal.internal.CanSignal
import zio.temporal.internal.ClassTagUtils
import zio.temporal.internal.TemporalInteraction
import zio.temporal.internal.tagging.Proxies
import zio.temporal.query.ZWorkflowStubQuerySyntax
import zio.temporal.signal.ZWorkflowStubSignalSyntax
import scala.language.experimental.macros
import scala.reflect.ClassTag

/** TODO: add
  *   - result(timeout: Duration)
  */
sealed trait ZWorkflowStub extends CanSignal[WorkflowStub] {

  override protected[zio] def signalMethod(signalName: String, args: Seq[AnyRef]): Unit =
    toJava.signal(signalName, args: _*)

  /** Fetches workflow result
    *
    * @tparam V
    *   expected workflow result type
    * @return
    *   either interaction error or the workflow result
    */
  def result[V: ClassTag]: TemporalIO[V] =
    TemporalInteraction.fromFuture {
      toJava.getResultAsync(ClassTagUtils.classOf[V])
    }

  /** Cancels workflow execution
    */
  def cancel: TemporalIO[Unit] =
    TemporalInteraction.from {
      toJava.cancel()
    }

  /** Terminates workflow execution
    *
    * @param reason
    *   termination reason which will be displayed in temporal web UI
    * @param details
    *   additional information
    */
  def terminate(reason: String, details: Any*): TemporalIO[Unit] =
    TemporalInteraction.from {
      toJava.terminate(reason, (details.asInstanceOf[Seq[AnyRef]]): _*)
    }
}

/** Represents untyped workflow stub
  *
  * @see
  *   [[WorkflowStub]]
  */
final class ZWorkflowStubImpl @internalApi() (val toJava: WorkflowStub) extends ZWorkflowStub

object ZWorkflowStub
    extends Proxies[ZWorkflowStub]
    with ZWorkflowExecutionSyntax
    with ZWorkflowStubSignalSyntax
    with ZWorkflowStubQuerySyntax {

  final implicit class Ops[A](private val self: ZWorkflowStub.Of[A]) extends AnyVal {

    /** Converts typed stub [[A]] to [[WorkflowStub]]
      *
      * @return
      *   untyped workflow stub
      */
    def toStub: ZWorkflowStub = new ZWorkflowStubImpl(WorkflowStub.fromTyped(self))
  }
}

package zio.temporal.workflow

import io.temporal.client.WorkflowStub
import zio.temporal.TemporalClientError
import zio.temporal.TemporalError
import zio.temporal.TemporalIO
import zio.temporal.internal.CanSignal
import zio.temporal.internal.ClassTagUtils
import zio.temporal.internal.TemporalInteraction
import zio.temporal.internal.tagging.Proxies
import zio.temporal.query.ZWorkflowStubQuerySyntax
import zio.temporal.signal.ZWorkflowStubSignalSyntax
import scala.language.experimental.macros
import scala.reflect.ClassTag

/** Represents untyped workflow stub
  *
  * @see
  *   [[WorkflowStub]]
  */
class ZWorkflowStub private[zio] (val toJava: WorkflowStub) extends AnyVal with CanSignal[WorkflowStub] {

  override protected[zio] def signalMethod(signalName: String, args: Seq[AnyRef]): Unit =
    toJava.signal(signalName, args: _*)

  /** Fetches workflow result
    *
    * @tparam V
    *   expected workflow result type
    * @return
    *   either interaction error or the workflow result
    */
  def result[V: ClassTag]: TemporalIO[TemporalClientError, V] =
    TemporalInteraction.fromFuture {
      toJava.getResultAsync(ClassTagUtils.classOf[V])
    }

  /** Fetches workflow result
    *
    * @tparam V
    *   expected workflow result type
    * @tparam E
    *   expected workflow business error type
    * @return
    *   either error or the workflow result
    */
  def resultEither[E: ClassTag, V: ClassTag]: TemporalIO[TemporalError[E], V] =
    TemporalInteraction.fromFutureEither {
      toJava.getResultAsync(ClassTagUtils.classOf[Either[E, V]])
    }

  /** Cancels workflow execution
    */
  def cancel: TemporalIO[TemporalClientError, Unit] =
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
  def terminate(reason: String, details: Any*): TemporalIO[TemporalClientError, Unit] =
    TemporalInteraction.from {
      toJava.terminate(reason, (details.asInstanceOf[Seq[AnyRef]]): _*)
    }
}

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
    def toStub: ZWorkflowStub = new ZWorkflowStub(WorkflowStub.fromTyped(self))
  }
}

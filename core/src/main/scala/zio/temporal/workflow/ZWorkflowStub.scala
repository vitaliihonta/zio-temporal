package zio.temporal.workflow

import zio.Duration
import io.temporal.client.WorkflowStub
import zio.temporal.{TemporalIO, internalApi}
import zio.temporal.internal.CanSignal
import zio.temporal.internal.ClassTagUtils
import zio.temporal.internal.TemporalInteraction
import zio.temporal.internal.tagging.Stubs
import zio.temporal.query.ZWorkflowStubQuerySyntax
import zio.temporal.signal.ZWorkflowStubSignalSyntax
import java.util.concurrent.TimeUnit
import scala.concurrent.TimeoutException
import scala.language.experimental.macros
import scala.reflect.ClassTag

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

  def result[V: ClassTag](timeout: Duration): TemporalIO[Option[V]] =
    TemporalInteraction.fromFutureTimeout {
      toJava.getResultAsync(timeout.toNanos, TimeUnit.NANOSECONDS, ClassTagUtils.classOf[V])
    }

  /** Request cancellation of a workflow execution.
    *
    * <p>Cancellation cancels [[io.temporal.workflow.CancellationScope]] that wraps the main workflow method. Note that
    * workflow can take long time to get canceled or even completely ignore the cancellation request.
    *
    * @throws WorkflowNotFoundException
    *   if the workflow execution doesn't exist or is already completed
    * @throws WorkflowServiceException
    *   for all other failures including networking and service availability issues
    */
  def cancel: TemporalIO[Unit] =
    TemporalInteraction.from {
      toJava.cancel()
    }

  /** Terminates a workflow execution.
    *
    * <p>Termination is a hard stop of a workflow execution which doesn't give workflow code any chance to perform
    * cleanup.
    *
    * @param reason
    *   optional reason for the termination request
    * @param details
    *   additional details about the termination reason
    * @throws WorkflowNotFoundException
    *   if the workflow execution doesn't exist or is already completed
    * @throws WorkflowServiceException
    *   for all other failures including networking and service availability issues
    */
  def terminate(reason: Option[String], details: Any*): TemporalIO[Unit] =
    TemporalInteraction.from {
      toJava.terminate(reason.orNull, details.asInstanceOf[Seq[AnyRef]]: _*)
    }
}

/** Represents untyped workflow stub
  *
  * @see
  *   [[WorkflowStub]]
  */
final class ZWorkflowStubImpl @internalApi() (val toJava: WorkflowStub) extends ZWorkflowStub

object ZWorkflowStub
    extends Stubs[ZWorkflowStub]
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

package zio.temporal.workflow

import zio.Duration
import io.temporal.client.WorkflowStub
import zio.temporal.{JavaTypeTag, TemporalIO, TypeIsSpecified, ZWorkflowExecution, internalApi}
import zio.temporal.internal.{BasicStubOps, Stubs, TemporalInteraction}
import zio.temporal.query.ZWorkflowStubQuerySyntax
import zio.temporal.signal.{ZWorkflowClientSignalWithStartSyntax, ZWorkflowStubSignalSyntax}

import java.util.concurrent.TimeUnit

sealed trait ZWorkflowStub extends BasicStubOps with ZWorkflowClientSignalWithStartSyntax {
  def toJava: WorkflowStub

  def untyped: ZWorkflowStub.Untyped

  /** Fetches workflow result
    *
    * @tparam V
    *   expected workflow result type
    * @return
    *   either interaction error or the workflow result
    */
  def result[V: TypeIsSpecified: JavaTypeTag]: TemporalIO[V] =
    untyped.result[V]

  def result[V: TypeIsSpecified: JavaTypeTag](timeout: Duration): TemporalIO[Option[V]] =
    untyped.result[V](timeout)

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
    untyped.cancel

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
    untyped.terminate(reason, details: _*)
}

/** Represents untyped workflow stub
  *
  * @see
  *   [[WorkflowStub]]
  */
final class ZWorkflowStubImpl @internalApi() (val toJava: WorkflowStub, val stubbedClass: Class[_])
    extends ZWorkflowStub { self =>
  override val untyped: ZWorkflowStub.Untyped = new ZWorkflowStub.UntypedImpl(toJava)
}

object ZWorkflowStub
    extends Stubs[ZWorkflowStub]
    with ZWorkflowExecutionSyntax
    with ZWorkflowStubSignalSyntax
    with ZWorkflowStubQuerySyntax {

  /** An untyped version of [[ZWorkflowStub]]
    */
  sealed trait Untyped {
    def toJava: WorkflowStub

    /** Synchronously signals a workflow by invoking its signal handler. Usually a signal handler is a method annotated
      * with [[zio.temporal.signalMethod]].
      *
      * @param signalName
      *   name of the signal handler. Usually it is a method name.
      * @param args
      *   signal method arguments
      * @throws WorkflowNotFoundException
      *   if the workflow execution doesn't exist or completed and can't be signalled
      * @throws WorkflowServiceException
      *   for all other failures including networking and service availability issues
      */
    def signal(signalName: String, args: Any*): TemporalIO[Unit]

    def start(args: Any*): TemporalIO[ZWorkflowExecution]

    def signalWithStart(
      signalName: String,
      signalArgs: Seq[Any],
      startArgs:  Seq[Any]
    ): TemporalIO[ZWorkflowExecution]

    /** Synchronously queries workflow by invoking its query handler. Usually a query handler is a method annotated with
      * [[zio.temporal.queryMethod]].
      *
      * @tparam R
      *   type of the query result
      * @param queryType
      *   name of the query handler. Usually it is a method name.
      * @param args
      *   optional query arguments
      * @return
      *   query result
      * @throws WorkflowNotFoundException
      *   if the workflow execution doesn't exist
      * @throws WorkflowQueryException
      *   if the query failed during it's execution by the workflow worker or was rejected on any stage
      * @throws WorkflowServiceException
      *   for all other failures including networking and service availability issues
      */
    def query[R: TypeIsSpecified: JavaTypeTag](queryType: String, args: Any*): TemporalIO[R]

    /** Fetches workflow result
      *
      * @tparam V
      *   expected workflow result type
      * @return
      *   either interaction error or the workflow result
      */
    def result[V: TypeIsSpecified: JavaTypeTag]: TemporalIO[V]

    def execute[V: TypeIsSpecified: JavaTypeTag](args: Any*): TemporalIO[V] =
      start(args: _*) *> result[V]

    def result[V: TypeIsSpecified: JavaTypeTag](timeout: Duration): TemporalIO[Option[V]]

    def executeWithTimeout[V: TypeIsSpecified: JavaTypeTag](timeout: Duration, args: Any*): TemporalIO[Option[V]] =
      start(args: _*) *> result[V](timeout)

    /** Request cancellation of a workflow execution.
      *
      * <p>Cancellation cancels [[io.temporal.workflow.CancellationScope]] that wraps the main workflow method. Note
      * that workflow can take long time to get canceled or even completely ignore the cancellation request.
      *
      * @throws WorkflowNotFoundException
      *   if the workflow execution doesn't exist or is already completed
      * @throws WorkflowServiceException
      *   for all other failures including networking and service availability issues
      */
    def cancel: TemporalIO[Unit]

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
    def terminate(reason: Option[String], details: Any*): TemporalIO[Unit]
  }

  private[temporal] final class UntypedImpl(val toJava: WorkflowStub) extends Untyped {
    override def signal(signalName: String, args: Any*): TemporalIO[Unit] =
      TemporalInteraction.from {
        toJava.signal(signalName, args.asInstanceOf[Seq[AnyRef]]: _*)
      }

    override def start(args: Any*): TemporalIO[ZWorkflowExecution] = {
      TemporalInteraction.from {
        new ZWorkflowExecution(
          toJava.start(args.asInstanceOf[Seq[AnyRef]]: _*)
        )
      }
    }

    override def signalWithStart(
      signalName: String,
      signalArgs: Seq[Any],
      startArgs:  Seq[Any]
    ): TemporalIO[ZWorkflowExecution] = {
      TemporalInteraction.from {
        new ZWorkflowExecution(
          toJava.signalWithStart(
            signalName,
            signalArgs.asInstanceOf[Seq[AnyRef]].toArray,
            startArgs.asInstanceOf[Seq[AnyRef]].toArray
          )
        )
      }
    }

    override def query[R: TypeIsSpecified: JavaTypeTag](queryType: String, args: Any*): TemporalIO[R] = {
      TemporalInteraction.from {
        toJava
          .query[R](queryType, JavaTypeTag[R].klass, JavaTypeTag[R].genericType, args.asInstanceOf[Seq[AnyRef]]: _*)
      }
    }

    override def result[V: TypeIsSpecified: JavaTypeTag]: TemporalIO[V] =
      TemporalInteraction.fromFuture {
        toJava.getResultAsync(JavaTypeTag[V].klass, JavaTypeTag[V].genericType)
      }

    override def result[V: TypeIsSpecified: JavaTypeTag](timeout: Duration): TemporalIO[Option[V]] =
      TemporalInteraction.fromFutureTimeout {
        toJava.getResultAsync(
          timeout.toNanos,
          TimeUnit.NANOSECONDS,
          JavaTypeTag[V].klass,
          JavaTypeTag[V].genericType
        )
      }

    override def cancel: TemporalIO[Unit] =
      TemporalInteraction.from {
        toJava.cancel()
      }

    override def terminate(reason: Option[String], details: Any*): TemporalIO[Unit] =
      TemporalInteraction.from {
        toJava.terminate(reason.orNull, details.asInstanceOf[Seq[AnyRef]]: _*)
      }
  }

  final implicit class Ops[A](private val self: ZWorkflowStub.Of[A]) extends AnyVal {}
}

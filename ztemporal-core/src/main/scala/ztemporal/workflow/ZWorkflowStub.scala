package ztemporal.workflow

import io.temporal.client.WorkflowStub
import ztemporal.func._
import ztemporal.internal.CanSignal
import ztemporal.internal.ClassTagUtils
import ztemporal.internal.TemporalInteraction
import ztemporal.internal.ZWorkflowQueryMacro
import ztemporal.ZTemporalClientError
import ztemporal.ZTemporalError
import ztemporal.ZTemporalIO
import ztemporal.internal.tagging.Tagged

import scala.reflect.ClassTag
import scala.language.experimental.macros

/** Represents untyped workflow stub
  *
  * @see [[WorkflowStub]]
  */
class ZWorkflowStub private[ztemporal] (override protected[ztemporal] val self: WorkflowStub)
    extends AnyVal
    with CanSignal[WorkflowStub] {

  override protected[ztemporal] def signalMethod(signalName: String, args: Seq[AnyRef]): Unit =
    self.signal(signalName, args: _*)

  /** Fetches workflow result
    *
    * @tparam V expected workflow result type
    * @return either interaction error or the workflow result
    */
  def result[V: ClassTag]: ZTemporalIO[ZTemporalClientError, V] =
    TemporalInteraction.fromFuture {
      self.getResultAsync(ClassTagUtils.classOf[V])
    }

  /** Fetches workflow result
    *
    * @tparam V expected workflow result type
    * @tparam E expected workflow business error type
    * @return either error or the workflow result
    */
  def resultEither[E: ClassTag, V: ClassTag]: ZTemporalIO[ZTemporalError[E], V] =
    TemporalInteraction.fromFutureEither {
      self.getResultAsync(ClassTagUtils.classOf[Either[E, V]])
    }

  /** Cancels workflow execution
    */
  def cancel: ZTemporalIO[ZTemporalClientError, Unit] =
    TemporalInteraction.from {
      self.cancel()
    }

  /** Terminates workflow execution
    *
    * @param reason termination reason which will be displayed in temporal web UI
    * @param details additional information
    */
  def terminate(reason: String, details: Any*): ZTemporalIO[ZTemporalClientError, Unit] =
    TemporalInteraction.from {
      self.terminate(reason, (details.asInstanceOf[Seq[AnyRef]]): _*)
    }

  /** Queries workflow state using provided [[io.temporal.workflow.QueryMethod]]
    *
    * @tparam Q workflow type
    * @tparam R query result
    * @return workflow query type
    */
  def query0[Q, R](f: Q => R): ZWorkflowQuery0[R] =
    macro ZWorkflowQueryMacro.queryImpl0[Q, R]

  /** Queries workflow state using provided [[io.temporal.workflow.QueryMethod]]
    *
    * @tparam Q workflow type
    * @tparam A query method parameter
    * @tparam R query result
    * @return workflow query type
    */
  def query[Q, A, R](f: Q => (A => R)): ZWorkflowQuery1[A, R] =
    macro ZWorkflowQueryMacro.queryImpl1[Q, A, R]

  /** Queries workflow state using provided [[io.temporal.workflow.QueryMethod]]
    *
    * @tparam Q workflow type
    * @tparam A first query method parameter
    * @tparam B second query method parameter
    * @tparam R query result
    * @return workflow query type
    */
  def query[Q, A, B, R](f: Q => ((A, B) => R)): ZWorkflowQuery2[A, B, R] =
    macro ZWorkflowQueryMacro.queryImpl2[Q, A, B, R]
}

object ZWorkflowStub extends Tagged {

  final implicit class Ops[A](private val self: ZWorkflowStub.Of[A]) extends AnyVal {

    /** Converts typed stub [[A]] to [[WorkflowStub]]
      *
      * @return untyped workflow stub
      */
    def toStub: ZWorkflowStub = new ZWorkflowStub(WorkflowStub.fromTyped(self))
  }
}

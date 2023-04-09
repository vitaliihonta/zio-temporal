package zio

import io.temporal.activity.ActivityInterface
import io.temporal.activity.ActivityMethod
import io.temporal.client.WorkflowException
import io.temporal.failure.TemporalException
import io.temporal.workflow.QueryMethod
import io.temporal.workflow.SignalMethod
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod
import zio.temporal.internal.ClassTagUtils

import scala.reflect.ClassTag

package object temporal {

  // Convenient aliases
  final type activityInterface = ActivityInterface
  final type activityMethod    = ActivityMethod
  final type workflowInterface = WorkflowInterface
  final type queryMethod       = QueryMethod
  final type signalMethod      = SignalMethod
  final type workflowMethod    = WorkflowMethod

  /** Alias for IO representing interaction with temporal server
    *
    * @tparam A
    *   the value type
    */
  final type TemporalIO[+A] = ZIO[Any, WorkflowException, A]

  /** Alias for IO representing interaction with temporal server
    *
    * @tparam R
    *   environment type
    *
    * @tparam A
    *   the value type
    */
  final type TemporalRIO[-R, +A] = ZIO[R, WorkflowException, A]

  /** Retrieves class name of a given type. Useful when specifying 'doNotRetry' errors in retry policies.
    * @see
    *   [[ZRetryOptions.withDoNotRetry]]
    */
  def nameOf[A: ClassTag]: String = ClassTagUtils.classOf[A].getName
}

package zio

import io.temporal.activity.ActivityInterface
import io.temporal.activity.ActivityMethod
import io.temporal.workflow.QueryMethod
import io.temporal.workflow.SignalMethod
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod

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
    * @tparam E
    *   ZIO Temporal error type
    * @tparam A
    *   the value type
    */
  final type TemporalIO[+E <: TemporalError[_], +A] = ZIO[Any, E, A]
}

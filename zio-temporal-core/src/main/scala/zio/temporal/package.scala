package zio

import io.temporal.activity.ActivityInterface
import io.temporal.activity.ActivityMethod
import io.temporal.workflow.QueryMethod
import io.temporal.workflow.SignalMethod
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod

package object temporal {

  // Convenient aliases
  type activity       = ActivityInterface
  type activityMethod = ActivityMethod
  type workflow       = WorkflowInterface
  type queryMethod    = QueryMethod
  type signalMethod   = SignalMethod
  type workflowMethod = WorkflowMethod

  /** Alias for IO representing interaction with temporal server
    *
    * @tparam E
    *   ZIO Temporal error type
    * @tparam A
    *   the value type
    */
  type TemporalIO[+E <: TemporalError[_], +A] = ZIO[Any, E, A]
}

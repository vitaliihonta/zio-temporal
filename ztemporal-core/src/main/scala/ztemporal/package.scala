import zio.ZIO
import zio.blocking.Blocking
import io.temporal.activity.ActivityInterface
import io.temporal.activity.ActivityMethod
import io.temporal.workflow.QueryMethod
import io.temporal.workflow.SignalMethod
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod

package object ztemporal {

  // Convenient aliases
  type activity       = ActivityInterface
  type activityMethod = ActivityMethod
  type workflow       = WorkflowInterface
  type queryMethod    = QueryMethod
  type signalMethod   = SignalMethod
  type workflowMethod = WorkflowMethod

  /** Alias for IO representing interaction with temporal server
    *
    * @tparam E ztemporal error type
    * @tparam A the value type
    */
  type ZTemporalIO[+E <: ZTemporalError[_], +A] = ZIO[Blocking, E, A]
}

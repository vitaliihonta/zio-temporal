package zio

import io.temporal.activity.ActivityInterface
import io.temporal.activity.ActivityMethod
import io.temporal.failure.TemporalException
import io.temporal.workflow.QueryMethod
import io.temporal.workflow.SignalMethod
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod
import zio.temporal.internal.ClassTagUtils
import scala.reflect.ClassTag
import scala.language.implicitConversions

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
  final type TemporalIO[+A] = ZIO[Any, TemporalException, A]

  /** Alias for IO representing interaction with temporal server
    *
    * @tparam R
    *   environment type
    *
    * @tparam A
    *   the value type
    */
  final type TemporalRIO[-R, +A] = ZIO[R, TemporalException, A]

  /** Retrieves class name of a given type. Useful when specifying 'doNotRetry' errors in retry policies.
    * @see
    *   [[ZRetryOptions.withDoNotRetry]]
    */
  def nameOf[A: ClassTag]: String = ClassTagUtils.classOf[A].getName

  /** Retrieves simple class name of a given type. Useful when specifying creating untyped stubs.
    */
  def simpleNameOf[A: ClassTag]: String = ClassTagUtils.classOf[A].getSimpleName

  /** Brings aspects `@@` to [[zio.ZLayer]]
    */
  implicit def ZLayerAspectSyntax[RIn, E, ROut](self: ZLayer[RIn, E, ROut]): ZLayerAspect.Syntax[RIn, E, ROut] =
    new ZLayerAspect.Syntax[RIn, E, ROut](self)

  /** Temporal uses `tally-core` that has it's own [[com.uber.m3.util.Duration]] class. This method converts
    * [[zio.Duration]] so that consumers won't need to use tally's one.
    */
  implicit def toTallyDuration(duration: zio.Duration): com.uber.m3.util.Duration =
    com.uber.m3.util.Duration.ofNanos(duration.toNanos)
}

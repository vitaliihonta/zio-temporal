package zio.temporal.activity

import zio._
import io.temporal.activity.ActivityCancellationType
import io.temporal.activity.ActivityOptions
import io.temporal.common.VersioningIntent
import io.temporal.common.context.ContextPropagator
import zio.temporal.ZRetryOptions
import scala.jdk.CollectionConverters._

@deprecated("Build ZActivityOptions and provide it directly", since = "0.6.0")
object ZActivityStubBuilderInitial {
  type Of[A]   = ZActivityStubBuilderInitial[ZActivityStub.Of[A]]
  type Untyped = ZActivityStubBuilderInitial[ZActivityStub.Untyped]
}

@deprecated("Build ZActivityOptions and provide it directly", since = "0.6.0")
final class ZActivityStubBuilderInitial[Res] private[zio] (buildImpl: ActivityOptions => Res) {

  /** Maximum time of a single Activity attempt.
    *
    * <p>Note that the Temporal Server doesn't detect Worker process failures directly. It relies on this timeout to
    * detect that an Activity that didn't complete on time. So this timeout should be as short as the longest possible
    * execution of the Activity body. Potentially long-running Activities must specify HeartbeatTimeout and call
    * [[ZActivityExecutionContext.heartbeat]] periodically for timely failure detection.
    *
    * <p>If [[withScheduleToCloseTimeout]] is not provided, then this timeout is required.
    */
  def withStartToCloseTimeout(timeout: Duration): ZActivityStubBuilder[Res] =
    new ZActivityStubBuilder[Res](buildImpl, _.setStartToCloseTimeout(timeout))

  /** Total time that a workflow is willing to wait for an Activity to complete.
    *
    * <p>ScheduleToCloseTimeout limits the total time of an Activity's execution including retries
    * [[withStartToCloseTimeout]] to limit the time of a single attempt).
    *
    * <p>Either this option or [[withStartToCloseTimeout]] is required.
    *
    * <p>Defaults to unlimited, which is chosen if set to null.
    */
  def withScheduleToCloseTimeout(timeout: Duration): ZActivityStubBuilder[Res] =
    new ZActivityStubBuilder[Res](buildImpl, _.setScheduleToCloseTimeout(timeout))
}

@deprecated("Build ZActivityOptions and provide it directly", since = "0.6.0")
final class ZActivityStubBuilder[Res] private[zio] (
  buildImpl:    ActivityOptions => Res,
  buildOptions: ActivityOptions.Builder => ActivityOptions.Builder) {

  private def copy(options: ActivityOptions.Builder => ActivityOptions.Builder): ZActivityStubBuilder[Res] =
    new ZActivityStubBuilder[Res](buildImpl, buildOptions andThen options)

  /** Maximum time of a single Activity attempt.
    *
    * <p>Note that the Temporal Server doesn't detect Worker process failures directly. It relies on this timeout to
    * detect that an Activity that didn't complete on time. So this timeout should be as short as the longest possible
    * execution of the Activity body. Potentially long-running Activities must specify HeartbeatTimeout and call
    * [[ZActivityExecutionContext.heartbeat]] periodically for timely failure detection.
    *
    * <p>If [[withScheduleToCloseTimeout]] is not provided, then this timeout is required.
    */
  def withStartToCloseTimeout(timeout: Duration): ZActivityStubBuilder[Res] =
    new ZActivityStubBuilder[Res](buildImpl, _.setStartToCloseTimeout(timeout))

  /** Total time that a workflow is willing to wait for an Activity to complete.
    *
    * <p>ScheduleToCloseTimeout limits the total time of an Activity's execution including retries
    * [[withStartToCloseTimeout]] to limit the time of a single attempt).
    *
    * <p>Either this option or [[withStartToCloseTimeout]] is required.
    *
    * <p>Defaults to unlimited, which is chosen if set to null.
    */
  def withScheduleToCloseTimeout(timeout: Duration): ZActivityStubBuilder[Res] =
    copy(_.setScheduleToCloseTimeout(timeout.asJava))

  /** Time that the Activity Task can stay in the Task Queue before it is picked up by a Worker.
    *
    * <p>ScheduleToStartTimeout is always non-retryable. Retrying after this timeout doesn't make sense as it would just
    * put the Activity Task back into the same Task Queue.
    *
    * <p>Defaults to unlimited.
    */
  def withScheduleToStartTimeout(timeout: Duration): ZActivityStubBuilder[Res] =
    copy(_.setScheduleToStartTimeout(timeout.asJava))

  /** Heartbeat interval. Activity must call [[ZActivityExecutionContext.heartbeat]] before this interval passes after
    * the last heartbeat or the Activity starts.
    */
  def withHeartbeatTimeout(timeout: Duration): ZActivityStubBuilder[Res] =
    copy(_.setHeartbeatTimeout(timeout.asJava))

  /** Task queue to use when dispatching activity task to a worker. By default, it is the same task list name the
    * workflow was started with.
    */
  def withTaskQueue(taskQueue: String): ZActivityStubBuilder[Res] =
    copy(_.setTaskQueue(taskQueue))

  /** RetryOptions that define how an Activity is retried in case of failure.
    *
    * <p>If not provided, the server-defined default activity retry policy will be used. If not overridden, the server
    * default activity retry policy is:
    *
    * <pre><code> InitialInterval: 1 second BackoffCoefficient: 2 MaximumInterval: 100 seconds // 100 * InitialInterval
    * MaximumAttempts: 0 // Unlimited NonRetryableErrorTypes: [] </pre></code>
    *
    * <p>If both [[withScheduleToStartTimeout]] and [[ZRetryOptions.withMaximumAttempts]] are not set, the Activity will
    * not be retried.
    *
    * <p>To ensure zero retries, set [[ZRetryOptions.withMaximumAttempts]] to 1.
    */
  def withRetryOptions(options: ZRetryOptions): ZActivityStubBuilder[Res] =
    copy(_.setRetryOptions(options.toJava))

  /** Note: <br> This method has extremely limited usage. The majority of users should just set
    * [[zio.temporal.workflow.ZWorkflowClientOptions.withContextPropagators]]
    *
    * <p>Both "client" (workflow worker) and "server" (activity worker) sides of context propagation from a workflow to
    * an activity exist in a worker process (potentially the same one), so they typically share the same worker options.
    * Specifically, [[ContextPropagator]]s specified on
    * [[zio.temporal.workflow.ZWorkflowClientOptions.withContextPropagators]]. <p>
    * [[zio.temporal.workflow.ZWorkflowClientOptions.withContextPropagators]] is the right place to specify
    * [[ContextPropagator]]s between Workflow and an Activity. <br> Specifying context propagators with this method
    * overrides them only on the "client" (workflow) side and can't be automatically promoted to the "server" (activity
    * worker), which always uses [[ContextPropagator]]s from
    * [[zio.temporal.workflow.ZWorkflowClientOptions.contextPropagators]] <br> The only legitimate usecase for this
    * method is probably a situation when the specific activity is implemented in a different language and in a
    * completely different worker codebase and in that case setting a [[ContextPropagator]] that is applied only on a
    * "client" side could make sense. <br> This is also why there is no equivalent method on Local activity options.
    *
    * @see
    *   <a href="https://github.com/temporalio/sdk-java/issues/490">Rejected feature reqest for
    *   LocalActivityOption#contextPropagators</a>
    * @param propagators
    *   specifies the list of context propagators to use during propagation from a workflow to the activity with these
    *   activity options. This list overrides the list specified on
    *   [[zio.temporal.workflow.ZWorkflowClientOptions.contextPropagators]]
    */
  def withContextPropagators(propagators: List[ContextPropagator]): ZActivityStubBuilder[Res] =
    copy(_.setContextPropagators(propagators.asJava))

  /** @see
    *   [[withContextPropagators]]
    */
  def withContextPropagators(propagators: ContextPropagator*): ZActivityStubBuilder[Res] =
    withContextPropagators(propagators.toList)

  /** In case of an activity's scope cancellation the corresponding activity stub call fails with a
    * [[zio.temporal.failure.CanceledFailure]]
    *
    * @param cancellationType
    *   Defines the activity's stub cancellation mode. The default value is [[ActivityCancellationType.TRY_CANCEL]]
    * @see
    *   ActivityCancellationType
    */
  def withCancellationType(cancellationType: ActivityCancellationType): ZActivityStubBuilder[Res] =
    copy(_.setCancellationType(cancellationType))

  /** If set to true, will not request eager execution regardless of worker settings. If false, eager execution may
    * still be disabled at the worker level or eager execution may not be requested due to lack of available slots.
    *
    * <p>Eager activity execution means the server returns requested eager activities directly from the workflow task
    * back to this worker which is faster than non-eager which may be dispatched to a separate worker.
    *
    * <p>Defaults to false, meaning that eager activity execution will be requested if possible.
    */
  def withDisableEagerExecution(value: Boolean): ZActivityStubBuilder[Res] =
    copy(_.setDisableEagerExecution(value))

  /** Specifies whether this activity should run on a worker with a compatible Build Id or not.
    *
    * @see
    *   [[VersioningIntent]]
    */
  def withVersioningIntent(value: VersioningIntent): ZActivityStubBuilder[Res] =
    copy(_.setVersioningIntent(value))

  /** Allows to specify options directly on the java SDK's [[ActivityOptions]]. Use it in case an appropriate `withXXX`
    * method is missing
    *
    * @note
    *   the options specified via this method take precedence over those specified via other methods.
    */
  def transformJavaOptions(
    f: ActivityOptions.Builder => ActivityOptions.Builder
  ): ZActivityStubBuilder[Res] = copy(f)

  /** Builds ActivityStub
    * @return
    *   activity stub
    */
  def build: Res = {
    val options = buildOptions(
      ActivityOptions.newBuilder()
    ).build()

    buildImpl(options)
  }
}

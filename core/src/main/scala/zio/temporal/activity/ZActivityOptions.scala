package zio.temporal.activity

import io.temporal.activity.{ActivityCancellationType, ActivityOptions}
import io.temporal.common.VersioningIntent
import io.temporal.common.context.ContextPropagator
import zio._
import zio.temporal.ZRetryOptions
import scala.jdk.CollectionConverters._

/** Options used to configure how an activity is invoked. */
final case class ZActivityOptions private[zio] (
  heartbeatTimeout:                     Option[Duration],
  scheduleToCloseTimeout:               Option[Duration],
  scheduleToStartTimeout:               Option[Duration],
  startToCloseTimeout:                  Option[Duration],
  taskQueue:                            Option[String],
  retryOptions:                         Option[ZRetryOptions],
  contextPropagators:                   List[ContextPropagator],
  cancellationType:                     Option[ActivityCancellationType],
  disableEagerExecution:                Option[Boolean],
  versioningIntent:                     Option[VersioningIntent],
  private val javaOptionsCustomization: ActivityOptions.Builder => ActivityOptions.Builder) {

  /** Maximum time of a single Activity attempt.
    *
    * <p>Note that the Temporal Server doesn't detect Worker process failures directly. It relies on this timeout to
    * detect that an Activity that didn't complete on time. So this timeout should be as short as the longest possible
    * execution of the Activity body. Potentially long-running Activities must specify HeartbeatTimeout and call
    * [[ZActivityExecutionContext.heartbeat]] periodically for timely failure detection.
    *
    * <p>If [[withScheduleToCloseTimeout]] is not provided, then this timeout is required.
    */
  def withStartToCloseTimeout(value: Duration): ZActivityOptions =
    copy(startToCloseTimeout = Some(value))

  /** Total time that a workflow is willing to wait for an Activity to complete.
    *
    * <p>ScheduleToCloseTimeout limits the total time of an Activity's execution including retries
    * [[withStartToCloseTimeout]] to limit the time of a single attempt).
    *
    * <p>Either this option or [[withStartToCloseTimeout]] is required.
    *
    * <p>Defaults to unlimited, which is chosen if set to null.
    */
  def withScheduleToCloseTimeout(value: Duration): ZActivityOptions =
    copy(scheduleToCloseTimeout = Some(value))

  /** Time that the Activity Task can stay in the Task Queue before it is picked up by a Worker.
    *
    * <p>ScheduleToStartTimeout is always non-retryable. Retrying after this timeout doesn't make sense as it would just
    * put the Activity Task back into the same Task Queue.
    *
    * <p>Defaults to unlimited.
    */
  def withScheduleToStartTimeout(value: Duration): ZActivityOptions =
    copy(scheduleToStartTimeout = Some(value))

  /** Heartbeat interval. Activity must call [[ZActivityExecutionContext.heartbeat]] before this interval passes after
    * the last heartbeat or the Activity starts.
    */
  def withHeartbeatTimeout(value: Duration): ZActivityOptions =
    copy(heartbeatTimeout = Some(value))

  /** Task queue to use when dispatching activity task to a worker. By default, it is the same task list name the
    * workflow was started with.
    */
  def withTaskQueue(value: String): ZActivityOptions =
    copy(taskQueue = Some(value))

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
  def withRetryOptions(options: ZRetryOptions): ZActivityOptions =
    copy(retryOptions = Some(options))

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
    * @param values
    *   specifies the list of context propagators to use during propagation from a workflow to the activity with these
    *   activity options. This list overrides the list specified on
    *   [[zio.temporal.workflow.ZWorkflowClientOptions.contextPropagators]]
    */
  def withContextPropagators(values: List[ContextPropagator]): ZActivityOptions =
    copy(contextPropagators = values)

  /** @see
    *   [[withContextPropagators]]
    */
  def withContextPropagators(values: ContextPropagator*): ZActivityOptions =
    withContextPropagators(values.toList)

  /** In case of an activity's scope cancellation the corresponding activity stub call fails with a
    * [[zio.temporal.failure.CanceledFailure]]
    *
    * @param value
    *   Defines the activity's stub cancellation mode. The default value is [[ActivityCancellationType.TRY_CANCEL]]
    * @see
    *   ActivityCancellationType
    */
  def withCancellationType(value: ActivityCancellationType): ZActivityOptions =
    copy(cancellationType = Some(value))

  /** If set to true, will not request eager execution regardless of worker settings. If false, eager execution may
    * still be disabled at the worker level or eager execution may not be requested due to lack of available slots.
    *
    * <p>Eager activity execution means the server returns requested eager activities directly from the workflow task
    * back to this worker which is faster than non-eager which may be dispatched to a separate worker.
    *
    * <p>Defaults to false, meaning that eager activity execution will be requested if possible.
    */
  def withDisableEagerExecution(value: Boolean): ZActivityOptions =
    copy(disableEagerExecution = Some(value))

  /** Specifies whether this activity should run on a worker with a compatible Build Id or not.
    *
    * @see
    *   [[VersioningIntent]]
    */
  def withVersioningIntent(value: VersioningIntent): ZActivityOptions =
    copy(versioningIntent = Some(value))

  /** Allows to specify options directly on the java SDK's [[ActivityOptions]]. Use it in case an appropriate `withXXX`
    * method is missing
    *
    * @note
    *   the options specified via this method take precedence over those specified via other methods.
    */
  def transformJavaOptions(
    f: ActivityOptions.Builder => ActivityOptions.Builder
  ): ZActivityOptions = copy(javaOptionsCustomization = f)

  /** Convert to Java SDK's [[ActivityOptions]]
    */
  def toJava: ActivityOptions = {
    val builder = ActivityOptions.newBuilder()

    heartbeatTimeout.foreach(builder.setHeartbeatTimeout)
    scheduleToCloseTimeout.foreach(builder.setScheduleToCloseTimeout)
    scheduleToStartTimeout.foreach(builder.setScheduleToStartTimeout)
    startToCloseTimeout.foreach(builder.setStartToCloseTimeout)
    taskQueue.foreach(builder.setTaskQueue)
    retryOptions.foreach(o => builder.setRetryOptions(o.toJava))
    builder.setContextPropagators(contextPropagators.asJava)
    cancellationType.foreach(builder.setCancellationType)
    disableEagerExecution.foreach(builder.setDisableEagerExecution)
    versioningIntent.foreach(builder.setVersioningIntent)

    javaOptionsCustomization(builder).build()
  }

  override def toString: String = {
    s"ZActivityOptions(" +
      s"heartbeatTimeout=$heartbeatTimeout" +
      s", scheduleToCloseTimeout=$scheduleToCloseTimeout" +
      s", scheduleToStartTimeout=$scheduleToStartTimeout" +
      s", startToCloseTimeout=$startToCloseTimeout" +
      s", taskQueue=$taskQueue" +
      s", retryOptions=$retryOptions" +
      s", contextPropagators=$contextPropagators" +
      s", cancellationType=$cancellationType" +
      s", disableEagerExecution=$disableEagerExecution" +
      s", versioningIntent=$versioningIntent" +
      s")"
  }
}

object ZActivityOptions {

  /** Maximum time of a single Activity attempt.
    *
    * <p>Note that the Temporal Server doesn't detect Worker process failures directly. It relies on this timeout to
    * detect that an Activity that didn't complete on time. So this timeout should be as short as the longest possible
    * execution of the Activity body. Potentially long-running Activities must specify HeartbeatTimeout and call
    * [[ZActivityExecutionContext.heartbeat]] periodically for timely failure detection.
    *
    * <p>If [[withScheduleToCloseTimeout]] is not provided, then this timeout is required.
    */
  def withStartToCloseTimeout(value: Duration): ZActivityOptions =
    new ZActivityOptions(
      heartbeatTimeout = None,
      scheduleToCloseTimeout = None,
      scheduleToStartTimeout = None,
      startToCloseTimeout = Some(value),
      taskQueue = None,
      retryOptions = None,
      contextPropagators = Nil,
      cancellationType = None,
      disableEagerExecution = None,
      versioningIntent = None,
      javaOptionsCustomization = identity
    )

  /** Total time that a workflow is willing to wait for an Activity to complete.
    *
    * <p>ScheduleToCloseTimeout limits the total time of an Activity's execution including retries
    * [[withStartToCloseTimeout]] to limit the time of a single attempt).
    *
    * <p>Either this option or [[withStartToCloseTimeout]] is required.
    *
    * <p>Defaults to unlimited, which is chosen if set to null.
    */
  def withScheduleToCloseTimeout(value: Duration): ZActivityOptions =
    new ZActivityOptions(
      heartbeatTimeout = None,
      scheduleToCloseTimeout = Some(value),
      scheduleToStartTimeout = None,
      startToCloseTimeout = None,
      taskQueue = None,
      retryOptions = None,
      contextPropagators = Nil,
      cancellationType = None,
      disableEagerExecution = None,
      versioningIntent = None,
      javaOptionsCustomization = identity
    )
}

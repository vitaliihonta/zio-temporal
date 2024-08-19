package zio.temporal.worker

import zio._
import io.temporal.worker.WorkerOptions

/** Represents worker options
  *
  * @see
  *   [[WorkerOptions]]
  */
final case class ZWorkerOptions private[zio] (
  maxWorkerActivitiesPerSecond:            Option[Double],
  maxConcurrentActivityExecutionSize:      Option[Int],
  maxConcurrentWorkflowTaskExecutionSize:  Option[Int],
  maxConcurrentLocalActivityExecutionSize: Option[Int],
  maxTaskQueueActivitiesPerSecond:         Option[Double],
  maxConcurrentWorkflowTaskPollers:        Option[Int],
  maxConcurrentActivityTaskPollers:        Option[Int],
  localActivityWorkerOnly:                 Option[Boolean],
  defaultDeadlockDetectionTimeout:         Option[Duration],
  maxHeartbeatThrottleInterval:            Option[Duration],
  defaultHeartbeatThrottleInterval:        Option[Duration],
  stickyQueueScheduleToStartTimeout:       Option[Duration],
  disableEagerExecution:                   Option[Boolean],
  /*implies useBuildIdForVersioning as well*/
  buildId: Option[String],
  private val javaOptionsCustomization: WorkerOptions.Builder => WorkerOptions.Builder) {

  /** @param value
    *   Maximum number of activities started per second by this worker. Default is 0 which means unlimited. <p>Note that
    *   this is a per worker limit. Use [[withMaxTaskQueueActivitiesPerSecond]] to set per task queue limit across
    *   multiple workers.
    */
  def withMaxWorkerActivitiesPerSecond(value: Double): ZWorkerOptions =
    copy(maxWorkerActivitiesPerSecond = Some(value))

  /** @param value
    *   Maximum number of activities executed in parallel. Default is 200, which is chosen if set to zero.
    */
  def withMaxConcurrentActivityExecutionSize(value: Int): ZWorkerOptions =
    copy(maxConcurrentActivityExecutionSize = Some(value))

  /** @param value
    *   Maximum number of simultaneously executed workflow tasks. Default is 200, which is chosen if set to zero.
    *
    * <p>Note that this is not related to the total number of open workflows which do not need to be loaded in a worker
    * when they are not making state transitions.
    */
  def withMaxConcurrentWorkflowTaskExecutionSize(value: Int): ZWorkerOptions =
    copy(maxConcurrentWorkflowTaskExecutionSize = Some(value))

  /** @param value
    *   Maximum number of local activities executed in parallel. Default is 200, which is chosen if set to zero.
    */
  def withMaxConcurrentLocalActivityExecutionSize(value: Int): ZWorkerOptions =
    copy(maxConcurrentLocalActivityExecutionSize = Some(value))

  /** Optional: Sets the rate limiting on number of activities that can be executed per second. This is managed by the
    * server and controls activities per second for the entire task queue across all the workers. Notice that the number
    * is represented in double, so that you can set it to less than 1 if needed. For example, set the number to 0.1
    * means you want your activity to be executed once every 10 seconds. This can be used to protect down stream
    * services from flooding. The zero value of this uses the default value. Default is unlimited.
    */
  def withMaxTaskQueueActivitiesPerSecond(value: Double): ZWorkerOptions =
    copy(maxTaskQueueActivitiesPerSecond = Some(value))

  /** Sets the maximum number of simultaneous long poll requests to the Temporal Server to retrieve workflow tasks.
    * Changing this value will affect the rate at which the worker is able to consume tasks from a task queue.
    *
    * <p>Due to internal logic where pollers alternate between sticky and non-sticky queues, this value cannot be 1 and
    * will be adjusted to 2 if set to that value.
    *
    * <p>Default is 5, which is chosen if set to zero.
    */
  def withMaxConcurrentWorkflowTaskPollers(value: Int): ZWorkerOptions =
    copy(maxConcurrentWorkflowTaskPollers = Some(value))

  /** Number of simultaneous poll requests on activity task queue. Consider incrementing if the worker is not throttled
    * due to `MaxActivitiesPerSecond` or `MaxConcurrentActivityExecutionSize` options and still cannot keep up with the
    * request rate.
    *
    * <p>Default is 5, which is chosen if set to zero.
    */
  def withMaxConcurrentActivityTaskPollers(value: Int): ZWorkerOptions =
    copy(maxConcurrentActivityTaskPollers = Some(value))

  /** If set to true worker would only handle workflow tasks and local activities. Non-local activities will not be
    * executed by this worker.
    *
    * <p>Default is false.
    */
  def withLocalActivityWorkerOnly(value: Boolean): ZWorkerOptions =
    copy(localActivityWorkerOnly = Some(value))

  /** @param value
    *   time period in ms that will be used to detect workflows deadlock. Default is 1000ms, which is chosen if set to
    *   zero. <p>Specifies an amount of time in milliseconds that workflow tasks are allowed to execute without
    *   interruption. If workflow task runs longer than specified interval without yielding (like calling an Activity),
    *   it will fail automatically.
    * @see
    *   [[io.temporal.internal.sync.PotentialDeadlockException]]
    */
  def withDefaultDeadlockDetectionTimeout(value: Duration): ZWorkerOptions =
    copy(defaultDeadlockDetectionTimeout = Some(value))

  /** @param value
    *   the maximum amount of time between sending each pending heartbeat to the server. Regardless of heartbeat
    *   timeout, no pending heartbeat will wait longer than this amount of time to send. Default is 60s, which is chosen
    *   if set to null or 0.
    */
  def withMaxHeartbeatThrottleInterval(value: Duration): ZWorkerOptions =
    copy(maxHeartbeatThrottleInterval = Some(value))

  /** @param value
    *   the default amount of time between sending each pending heartbeat to the server. This is used if the
    *   ActivityOptions do not provide a HeartbeatTimeout. Otherwise, the interval becomes a value a bit smaller than
    *   the given HeartbeatTimeout. Default is 30s, which is chosen if set to null or 0.
    */
  def withDefaultHeartbeatThrottleInterval(value: Duration): ZWorkerOptions =
    copy(defaultHeartbeatThrottleInterval = Some(value))

  /** Timeout for a workflow task routed to the "sticky worker" - host that has the workflow instance cached in memory.
    * Once it times out, then it can be picked up by any worker.
    *
    * <p>Default value is 5 seconds.
    */
  def withStickyQueueScheduleToStartTimeout(value: Duration): ZWorkerOptions =
    copy(stickyQueueScheduleToStartTimeout = Some(value))

  /** Disable eager activities. If set to true, eager execution will not be requested for activities requested from
    * workflows bound to this Worker.
    *
    * <p>Eager activity execution means the server returns requested eager activities directly from the workflow task
    * back to this worker which is faster than non-eager which may be dispatched to a separate worker.
    *
    * <p>Defaults to false, meaning that eager activity execution is permitted
    */
  def withDisableEagerExecution(value: Boolean): ZWorkerOptions =
    copy(disableEagerExecution = Some(value))

  /** Set a unique identifier for this worker. The identifier should be stable with respect to the code the worker uses
    * for workflows, activities, and interceptors.
    */
  def withBuildId(value: String): ZWorkerOptions =
    copy(buildId = Some(value))

  /** Allows to specify options directly on the java SDK's [[WorkerOptions]]. Use it in case an appropriate `withXXX`
    * method is missing
    *
    * @note
    *   the options specified via this method take precedence over those specified via other methods.
    */
  def transformJavaOptions(
    f: WorkerOptions.Builder => WorkerOptions.Builder
  ): ZWorkerOptions =
    copy(javaOptionsCustomization = f)

  /** Converts worker options to Java SDK's [[WorkerOptions]]
    */
  def toJava: WorkerOptions = {
    val builder = WorkerOptions.newBuilder()

    maxWorkerActivitiesPerSecond.foreach(builder.setMaxWorkerActivitiesPerSecond)
    maxConcurrentActivityExecutionSize.foreach(builder.setMaxConcurrentActivityExecutionSize)
    maxConcurrentWorkflowTaskExecutionSize.foreach(builder.setMaxConcurrentWorkflowTaskExecutionSize)
    maxConcurrentLocalActivityExecutionSize.foreach(builder.setMaxConcurrentLocalActivityExecutionSize)
    maxTaskQueueActivitiesPerSecond.foreach(builder.setMaxTaskQueueActivitiesPerSecond)
    maxConcurrentWorkflowTaskPollers.foreach(builder.setMaxConcurrentWorkflowTaskPollers)
    maxConcurrentActivityTaskPollers.foreach(builder.setMaxConcurrentActivityTaskPollers)
    localActivityWorkerOnly.foreach(builder.setLocalActivityWorkerOnly)
    defaultDeadlockDetectionTimeout.foreach(v => builder.setDefaultDeadlockDetectionTimeout(v.toMillis))
    maxHeartbeatThrottleInterval.foreach(builder.setMaxHeartbeatThrottleInterval)
    defaultHeartbeatThrottleInterval.foreach(builder.setDefaultHeartbeatThrottleInterval)
    stickyQueueScheduleToStartTimeout.foreach(builder.setStickyQueueScheduleToStartTimeout)
    disableEagerExecution.foreach(builder.setDisableEagerExecution)
    buildId.foreach { buildId =>
      // flag must be set to true
      builder.setUseBuildIdForVersioning(true)
      builder.setBuildId(buildId)
    }

    javaOptionsCustomization(builder).build()
  }

  override def toString: String = {
    s"ZWorkerOptions(" +
      s"maxWorkerActivitiesPerSecond=$maxWorkerActivitiesPerSecond" +
      s", maxConcurrentActivityExecutionSize=$maxConcurrentActivityExecutionSize" +
      s", maxConcurrentWorkflowTaskExecutionSize=$maxConcurrentWorkflowTaskExecutionSize" +
      s", maxConcurrentLocalActivityExecutionSize=$maxConcurrentLocalActivityExecutionSize" +
      s", maxTaskQueueActivitiesPerSecond=$maxTaskQueueActivitiesPerSecond" +
      s", maxConcurrentWorkflowTaskPollers=$maxConcurrentWorkflowTaskPollers" +
      s", maxConcurrentActivityTaskPollers=$maxConcurrentActivityTaskPollers" +
      s", localActivityWorkerOnly=$localActivityWorkerOnly" +
      s", defaultDeadlockDetectionTimeout=$defaultDeadlockDetectionTimeout" +
      s", maxHeartbeatThrottleInterval=$maxHeartbeatThrottleInterval" +
      s", defaultHeartbeatThrottleInterval=$defaultHeartbeatThrottleInterval" +
      s", stickyQueueScheduleToStartTimeout=$stickyQueueScheduleToStartTimeout" +
      s", disableEagerExecution=$disableEagerExecution" +
      s", buildId=$buildId" +
      s")"
  }
}

object ZWorkerOptions {

  val default: ZWorkerOptions = new ZWorkerOptions(
    maxWorkerActivitiesPerSecond = None,
    maxConcurrentActivityExecutionSize = None,
    maxConcurrentWorkflowTaskExecutionSize = None,
    maxConcurrentLocalActivityExecutionSize = None,
    maxTaskQueueActivitiesPerSecond = None,
    maxConcurrentWorkflowTaskPollers = None,
    maxConcurrentActivityTaskPollers = None,
    localActivityWorkerOnly = None,
    defaultDeadlockDetectionTimeout = None,
    maxHeartbeatThrottleInterval = None,
    defaultHeartbeatThrottleInterval = None,
    stickyQueueScheduleToStartTimeout = None,
    javaOptionsCustomization = identity,
    disableEagerExecution = None,
    buildId = None
  )
}

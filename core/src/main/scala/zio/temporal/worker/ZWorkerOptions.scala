package zio.temporal.worker

import io.temporal.worker.WorkerOptions

/** Represents worker options
  *
  * @see
  *   [[WorkerOptions]]
  */
case class ZWorkerOptions private[zio] (
  maxWorkerActivitiesPerSecond:            Option[Double],
  maxConcurrentActivityExecutionSize:      Option[Int],
  maxConcurrentWorkflowTaskExecutionSize:  Option[Int],
  maxConcurrentLocalActivityExecutionSize: Option[Int],
  maxTaskQueueActivitiesPerSecond:         Option[Double],
  workflowPollThreadCount:                 Option[Int],
  activityPollThreadCount:                 Option[Int],
  localActivityWorkerOnly:                 Option[Boolean],
  private val javaOptionsCustomization:    WorkerOptions.Builder => WorkerOptions.Builder) {

  def withMaxWorkerActivitiesPerSecond(value: Double): ZWorkerOptions =
    copy(maxWorkerActivitiesPerSecond = Some(value))

  def withMaxConcurrentActivityExecutionSize(value: Int): ZWorkerOptions =
    copy(maxConcurrentActivityExecutionSize = Some(value))

  def withMaxConcurrentWorkflowTaskExecutionSize(value: Int): ZWorkerOptions =
    copy(maxConcurrentWorkflowTaskExecutionSize = Some(value))

  def withMaxConcurrentLocalActivityExecutionSize(value: Int): ZWorkerOptions =
    copy(maxConcurrentLocalActivityExecutionSize = Some(value))

  def withMaxTaskQueueActivitiesPerSecond(value: Double): ZWorkerOptions =
    copy(maxTaskQueueActivitiesPerSecond = Some(value))

  def withWorkflowPollThreadCount(value: Int): ZWorkerOptions =
    copy(workflowPollThreadCount = Some(value))

  def withActivityPollThreadCount(value: Int): ZWorkerOptions =
    copy(activityPollThreadCount = Some(value))

  def withLocalActivityWorkerOnly(value: Boolean): ZWorkerOptions =
    copy(localActivityWorkerOnly = Some(value))

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

  def toJava: WorkerOptions = {
    val builder = WorkerOptions.newBuilder()

    maxWorkerActivitiesPerSecond.foreach(builder.setMaxWorkerActivitiesPerSecond)
    maxConcurrentActivityExecutionSize.foreach(builder.setMaxConcurrentActivityExecutionSize)
    maxConcurrentWorkflowTaskExecutionSize.foreach(builder.setMaxConcurrentWorkflowTaskExecutionSize)
    maxConcurrentLocalActivityExecutionSize.foreach(builder.setMaxConcurrentLocalActivityExecutionSize)
    maxTaskQueueActivitiesPerSecond.foreach(builder.setMaxTaskQueueActivitiesPerSecond)
    workflowPollThreadCount.foreach(builder.setWorkflowPollThreadCount)
    activityPollThreadCount.foreach(builder.setActivityPollThreadCount)
    localActivityWorkerOnly.foreach(builder.setLocalActivityWorkerOnly)

    javaOptionsCustomization(builder).build()
  }
}

object ZWorkerOptions {

  val default: ZWorkerOptions = new ZWorkerOptions(
    maxWorkerActivitiesPerSecond = None,
    maxConcurrentActivityExecutionSize = None,
    maxConcurrentWorkflowTaskExecutionSize = None,
    maxConcurrentLocalActivityExecutionSize = None,
    maxTaskQueueActivitiesPerSecond = None,
    workflowPollThreadCount = None,
    activityPollThreadCount = None,
    localActivityWorkerOnly = None,
    javaOptionsCustomization = identity
  )
}

package zio.temporal.worker

import io.temporal.worker.WorkerOptions

/** Represents worker options
  *
  * @see
  *   [[WorkerOptions]]
  */
class ZWorkerOptions private (
  val maxWorkerActivitiesPerSecond:            Option[Double],
  val maxConcurrentActivityExecutionSize:      Option[Int],
  val maxConcurrentWorkflowTaskExecutionSize:  Option[Int],
  val maxConcurrentLocalActivityExecutionSize: Option[Int],
  val maxTaskQueueActivitiesPerSecond:         Option[Double],
  val workflowPollThreadCount:                 Option[Int],
  val activityPollThreadCount:                 Option[Int],
  val localActivityWorkerOnly:                 Option[Boolean]) {

  override def toString: String =
    s"ZWorkerOptions(" +
      s"maxWorkerActivitiesPerSecond=$maxWorkerActivitiesPerSecond, " +
      s"maxConcurrentActivityExecutionSize=$maxConcurrentActivityExecutionSize, " +
      s"maxConcurrentWorkflowTaskExecutionSize=$maxConcurrentWorkflowTaskExecutionSize, " +
      s"maxConcurrentLocalActivityExecutionSize=$maxConcurrentLocalActivityExecutionSize, " +
      s"maxTaskQueueActivitiesPerSecond=$maxTaskQueueActivitiesPerSecond, " +
      s"workflowPollThreadCount=$workflowPollThreadCount, " +
      s"activityPollThreadCount=$activityPollThreadCount, " +
      s"localActivityWorkerOnly=$localActivityWorkerOnly)"

  private def copy(
    _maxWorkerActivitiesPerSecond:            Option[Double] = maxWorkerActivitiesPerSecond,
    _maxConcurrentActivityExecutionSize:      Option[Int] = maxConcurrentActivityExecutionSize,
    _maxConcurrentWorkflowTaskExecutionSize:  Option[Int] = maxConcurrentWorkflowTaskExecutionSize,
    _maxConcurrentLocalActivityExecutionSize: Option[Int] = maxConcurrentLocalActivityExecutionSize,
    _maxTaskQueueActivitiesPerSecond:         Option[Double] = maxTaskQueueActivitiesPerSecond,
    _workflowPollThreadCount:                 Option[Int] = workflowPollThreadCount,
    _activityPollThreadCount:                 Option[Int] = activityPollThreadCount,
    _localActivityWorkerOnly:                 Option[Boolean] = localActivityWorkerOnly
  ): ZWorkerOptions =
    new ZWorkerOptions(
      _maxWorkerActivitiesPerSecond,
      _maxConcurrentActivityExecutionSize,
      _maxConcurrentWorkflowTaskExecutionSize,
      _maxConcurrentLocalActivityExecutionSize,
      _maxTaskQueueActivitiesPerSecond,
      _workflowPollThreadCount,
      _activityPollThreadCount,
      _localActivityWorkerOnly
    )

  def withMaxWorkerActivitiesPerSecond(value: Double): ZWorkerOptions =
    copy(_maxWorkerActivitiesPerSecond = Some(value))

  def withMaxConcurrentActivityExecutionSize(value: Int): ZWorkerOptions =
    copy(_maxConcurrentActivityExecutionSize = Some(value))

  def withMaxConcurrentWorkflowTaskExecutionSize(value: Int): ZWorkerOptions =
    copy(_maxConcurrentWorkflowTaskExecutionSize = Some(value))

  def withMaxConcurrentLocalActivityExecutionSize(value: Int): ZWorkerOptions =
    copy(_maxConcurrentLocalActivityExecutionSize = Some(value))

  def withMaxTaskQueueActivitiesPerSecond(value: Double): ZWorkerOptions =
    copy(_maxTaskQueueActivitiesPerSecond = Some(value))

  def withWorkflowPollThreadCount(value: Int): ZWorkerOptions =
    copy(_workflowPollThreadCount = Some(value))

  def withActivityPollThreadCount(value: Int): ZWorkerOptions =
    copy(_activityPollThreadCount = Some(value))

  def withLocalActivityWorkerOnly(value: Boolean): ZWorkerOptions =
    copy(_localActivityWorkerOnly = Some(value))

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

    builder.build()
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
    localActivityWorkerOnly = None
  )
}

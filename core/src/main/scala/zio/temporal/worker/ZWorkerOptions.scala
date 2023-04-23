package zio.temporal.worker

import zio.*
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
  maxConcurrentWorkflowTaskPollers:        Option[Int],
  maxConcurrentActivityTaskPollers:        Option[Int],
  localActivityWorkerOnly:                 Option[Boolean],
  stickyQueueScheduleToStartTimeout:       Option[Duration],
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

  def withMaxConcurrentWorkflowTaskPollers(value: Int): ZWorkerOptions =
    copy(maxConcurrentWorkflowTaskPollers = Some(value))

  def withMaxConcurrentActivityTaskPollers(value: Int): ZWorkerOptions =
    copy(maxConcurrentActivityTaskPollers = Some(value))

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
    maxConcurrentWorkflowTaskPollers.foreach(builder.setMaxConcurrentWorkflowTaskPollers)
    maxConcurrentActivityTaskPollers.foreach(builder.setMaxConcurrentActivityTaskPollers)
    localActivityWorkerOnly.foreach(builder.setLocalActivityWorkerOnly)
    stickyQueueScheduleToStartTimeout.foreach(builder.setStickyQueueScheduleToStartTimeout)

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
    maxConcurrentWorkflowTaskPollers = None,
    maxConcurrentActivityTaskPollers = None,
    localActivityWorkerOnly = None,
    stickyQueueScheduleToStartTimeout = None,
    javaOptionsCustomization = identity
  )
}

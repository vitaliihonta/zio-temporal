package ztemporal.worker

import io.temporal.common.interceptors.WorkerInterceptor
import io.temporal.worker.WorkerFactoryOptions
import scala.compat.java8.DurationConverters._
import scala.concurrent.duration.FiniteDuration

/** Represents worker factory options
  *
  * @see [[WorkerFactoryOptions]]
  */
class ZWorkerFactoryOptions private (
  workflowHostLocalTaskQueueScheduleToStartTimeout: Option[FiniteDuration],
  workflowCacheSize:                                Option[Int],
  maxWorkflowThreadCount:                           Option[Int],
  workerInterceptors:                               List[WorkerInterceptor],
  enableLoggingInReplay:                            Option[Boolean],
  workflowHostLocalPollThreadCount:                 Option[Int]) {

  override def toString: String =
    s"ZWorkerFactoryOptions(" +
      s"workflowHostLocalTaskQueueScheduleToStartTimeout=$workflowHostLocalTaskQueueScheduleToStartTimeout, " +
      s"workflowCacheSize=$workflowCacheSize, " +
      s"maxWorkflowThreadCount=$maxWorkflowThreadCount, " +
      s"workerInterceptors=$workerInterceptors, " +
      s"enableLoggingInReplay=$enableLoggingInReplay, " +
      s"workflowHostLocalPollThreadCount=$workflowHostLocalPollThreadCount)"

  def withWorkflowHostLocalTaskQueueScheduleToStartTimeout(value: FiniteDuration): ZWorkerFactoryOptions =
    copy(_workflowHostLocalTaskQueueScheduleToStartTimeout = Some(value))

  def withWorkflowCacheSize(value: Int): ZWorkerFactoryOptions =
    copy(_workflowCacheSize = Some(value))

  def withMaxWorkflowThreadCount(value: Int): ZWorkerFactoryOptions =
    copy(_maxWorkflowThreadCount = Some(value))

  def withWorkerInterceptors(value: WorkerInterceptor*): ZWorkerFactoryOptions =
    copy(_workerInterceptors = value.toList)

  def withEnableLoggingInReplay(value: Boolean): ZWorkerFactoryOptions =
    copy(_enableLoggingInReplay = Some(value))

  def withWorkflowHostLocalPollThreadCount(value: Int): ZWorkerFactoryOptions =
    copy(_workflowHostLocalPollThreadCount = Some(value))

  def toJava: WorkerFactoryOptions = {
    val builder = WorkerFactoryOptions.newBuilder()
    workflowHostLocalTaskQueueScheduleToStartTimeout.foreach(timeout =>
      builder.setWorkflowHostLocalTaskQueueScheduleToStartTimeout(timeout.toJava)
    )
    workflowCacheSize.foreach(builder.setWorkflowCacheSize)
    maxWorkflowThreadCount.foreach(builder.setMaxWorkflowThreadCount)
    builder.setWorkerInterceptors(workerInterceptors: _*)
    enableLoggingInReplay.foreach(builder.setEnableLoggingInReplay)
    workflowHostLocalPollThreadCount.foreach(builder.setWorkflowHostLocalPollThreadCount)
    builder.build()
  }

  private def copy(
    _workflowHostLocalTaskQueueScheduleToStartTimeout: Option[FiniteDuration] =
      workflowHostLocalTaskQueueScheduleToStartTimeout,
    _workflowCacheSize:                Option[Int] = workflowCacheSize,
    _maxWorkflowThreadCount:           Option[Int] = maxWorkflowThreadCount,
    _workerInterceptors:               List[WorkerInterceptor] = workerInterceptors,
    _enableLoggingInReplay:            Option[Boolean] = enableLoggingInReplay,
    _workflowHostLocalPollThreadCount: Option[Int] = workflowHostLocalPollThreadCount
  ) =
    new ZWorkerFactoryOptions(
      _workflowHostLocalTaskQueueScheduleToStartTimeout,
      _workflowCacheSize,
      _maxWorkflowThreadCount,
      _workerInterceptors,
      _enableLoggingInReplay,
      _workflowHostLocalPollThreadCount
    )
}

object ZWorkerFactoryOptions {

  val default: ZWorkerFactoryOptions = new ZWorkerFactoryOptions(
    workflowHostLocalTaskQueueScheduleToStartTimeout = None,
    workflowCacheSize = None,
    maxWorkflowThreadCount = None,
    workerInterceptors = Nil,
    enableLoggingInReplay = None,
    workflowHostLocalPollThreadCount = None
  )
}

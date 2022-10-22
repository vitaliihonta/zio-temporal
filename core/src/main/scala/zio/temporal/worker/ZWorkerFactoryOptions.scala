package zio.temporal.worker

import io.temporal.common.interceptors.WorkerInterceptor
import io.temporal.worker.WorkerFactoryOptions
import zio._

/** Represents worker factory options
  *
  * @see
  *   [[WorkerFactoryOptions]]
  */
case class ZWorkerFactoryOptions private[zio] (
  workflowHostLocalTaskQueueScheduleToStartTimeout: Option[Duration],
  workflowCacheSize:                                Option[Int],
  maxWorkflowThreadCount:                           Option[Int],
  workerInterceptors:                               List[WorkerInterceptor],
  enableLoggingInReplay:                            Option[Boolean],
  workflowHostLocalPollThreadCount:                 Option[Int],
  private val javaOptionsCustomization:             WorkerFactoryOptions.Builder => WorkerFactoryOptions.Builder) {

  def withWorkflowHostLocalTaskQueueScheduleToStartTimeout(value: Duration): ZWorkerFactoryOptions =
    copy(workflowHostLocalTaskQueueScheduleToStartTimeout = Some(value))

  def withWorkflowCacheSize(value: Int): ZWorkerFactoryOptions =
    copy(workflowCacheSize = Some(value))

  def withMaxWorkflowThreadCount(value: Int): ZWorkerFactoryOptions =
    copy(maxWorkflowThreadCount = Some(value))

  def withWorkerInterceptors(value: WorkerInterceptor*): ZWorkerFactoryOptions =
    copy(workerInterceptors = value.toList)

  def withEnableLoggingInReplay(value: Boolean): ZWorkerFactoryOptions =
    copy(enableLoggingInReplay = Some(value))

  def withWorkflowHostLocalPollThreadCount(value: Int): ZWorkerFactoryOptions =
    copy(workflowHostLocalPollThreadCount = Some(value))

  def toJava: WorkerFactoryOptions = {
    val builder = WorkerFactoryOptions.newBuilder()
    workflowHostLocalTaskQueueScheduleToStartTimeout.foreach(timeout =>
      builder.setWorkflowHostLocalTaskQueueScheduleToStartTimeout(timeout.asJava)
    )
    workflowCacheSize.foreach(builder.setWorkflowCacheSize)
    maxWorkflowThreadCount.foreach(builder.setMaxWorkflowThreadCount)
    builder.setWorkerInterceptors(workerInterceptors: _*)
    enableLoggingInReplay.foreach(builder.setEnableLoggingInReplay)
    workflowHostLocalPollThreadCount.foreach(builder.setWorkflowHostLocalPollThreadCount)
    javaOptionsCustomization(builder).build()
  }
}

object ZWorkerFactoryOptions {

  val default: ZWorkerFactoryOptions = new ZWorkerFactoryOptions(
    workflowHostLocalTaskQueueScheduleToStartTimeout = None,
    workflowCacheSize = None,
    maxWorkflowThreadCount = None,
    workerInterceptors = Nil,
    enableLoggingInReplay = None,
    workflowHostLocalPollThreadCount = None,
    javaOptionsCustomization = identity
  )
}

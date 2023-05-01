package zio.temporal.worker

import io.temporal.common.interceptors.WorkerInterceptor
import io.temporal.worker.WorkerFactoryOptions
import zio.*
import zio.temporal.internal.ConfigurationCompanion
import zio.temporal.workflow.ZWorkflowClientOptions

/** Represents worker factory options
  *
  * @see
  *   [[WorkerFactoryOptions]]
  */
case class ZWorkerFactoryOptions private[zio] (
  workflowCacheSize:                    Option[Int],
  maxWorkflowThreadCount:               Option[Int],
  workerInterceptors:                   List[WorkerInterceptor],
  enableLoggingInReplay:                Option[Boolean],
  private val javaOptionsCustomization: WorkerFactoryOptions.Builder => WorkerFactoryOptions.Builder) {

  def withWorkflowCacheSize(value: Int): ZWorkerFactoryOptions =
    copy(workflowCacheSize = Some(value))

  def withMaxWorkflowThreadCount(value: Int): ZWorkerFactoryOptions =
    copy(maxWorkflowThreadCount = Some(value))

  def withWorkerInterceptors(value: WorkerInterceptor*): ZWorkerFactoryOptions =
    copy(workerInterceptors = value.toList)

  def withEnableLoggingInReplay(value: Boolean): ZWorkerFactoryOptions =
    copy(enableLoggingInReplay = Some(value))

  /** Allows to specify options directly on the java SDK's [[WorkerFactoryOptions]]. Use it in case an appropriate
    * `withXXX` method is missing
    *
    * @note
    *   the options specified via this method take precedence over those specified via other methods.
    */
  def transformJavaOptions(
    f: WorkerFactoryOptions.Builder => WorkerFactoryOptions.Builder
  ): ZWorkerFactoryOptions =
    copy(javaOptionsCustomization = f)

  def toJava: WorkerFactoryOptions = {
    val builder = WorkerFactoryOptions.newBuilder()
    workflowCacheSize.foreach(builder.setWorkflowCacheSize)
    maxWorkflowThreadCount.foreach(builder.setMaxWorkflowThreadCount)
    builder.setWorkerInterceptors(workerInterceptors: _*)
    enableLoggingInReplay.foreach(builder.setEnableLoggingInReplay)
    javaOptionsCustomization(builder).build()
  }
}

object ZWorkerFactoryOptions extends ConfigurationCompanion[ZWorkerFactoryOptions] {

  def withWorkflowCacheSize(value: Int): Configure =
    configure(_.withWorkflowCacheSize(value))

  def withMaxWorkflowThreadCount(value: Int): Configure =
    configure(_.withMaxWorkflowThreadCount(value))

  def withWorkerInterceptors(value: WorkerInterceptor*): Configure =
    configure(_.withWorkerInterceptors(value: _*))

  def withEnableLoggingInReplay(value: Boolean): Configure =
    configure(_.withEnableLoggingInReplay(value))

  def transformJavaOptions(
    f: WorkerFactoryOptions.Builder => WorkerFactoryOptions.Builder
  ): Configure =
    configure(_.transformJavaOptions(f))

  private val workerFactoryConfig =
    Config.int("workflow_cache_size").optional ++
      Config.int("max_workflow_thread_count").optional ++
      Config.boolean("enable_logging_in_replay").optional

  /** Reads config from the default path `zio.temporal.ZWorkerFactory`
    */
  val make: Layer[Config.Error, ZWorkerFactoryOptions] =
    makeImpl(Nil)

  /** Allows to specify custom path for the config
    */
  def forPath(name: String, names: String*): Layer[Config.Error, ZWorkerFactoryOptions] =
    makeImpl(List(name) ++ names)

  private def makeImpl(additionalPath: List[String]): Layer[Config.Error, ZWorkerFactoryOptions] = {
    val config = additionalPath match {
      case Nil          => workerFactoryConfig.nested("zio", "temporal", "zworker_factory")
      case head :: tail => workerFactoryConfig.nested(head, tail: _*)
    }
    ZLayer.fromZIO {
      ZIO.config(config).map {
        case (
              workflowCacheSize,
              maxWorkflowThreadCount,
              enableLoggingInReplay
            ) =>
          new ZWorkerFactoryOptions(
            workflowCacheSize = workflowCacheSize,
            maxWorkflowThreadCount = maxWorkflowThreadCount,
            workerInterceptors = Nil,
            enableLoggingInReplay = enableLoggingInReplay,
            javaOptionsCustomization = identity
          )
      }
    }
  }
}

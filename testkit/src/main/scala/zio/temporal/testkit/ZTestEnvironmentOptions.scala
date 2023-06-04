package zio.temporal.testkit

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.uber.m3.tally.Scope
import io.temporal.common.converter._
import io.temporal.testing.TestEnvironmentOptions
import zio._
import zio.temporal.json.JacksonDataConverter
import zio.temporal.worker.ZWorkerFactoryOptions
import zio.temporal.workflow.ZWorkflowClientOptions

/** Represents ZTestEnvironment options.
  * @see
  *   [[TestEnvironmentOptions]]
  */
case class ZTestEnvironmentOptions private[zio] (
  workerFactoryOptions:                 ZWorkerFactoryOptions,
  workflowClientOptions:                ZWorkflowClientOptions,
  metricsScope:                         Option[Scope],
  useExternalService:                   Option[Boolean],
  target:                               Option[String],
  private val javaOptionsCustomization: TestEnvironmentOptions.Builder => TestEnvironmentOptions.Builder) {

  def withWorkerFactoryOptions(value: ZWorkerFactoryOptions): ZTestEnvironmentOptions =
    copy(workerFactoryOptions = value)

  def withWorkflowClientOptions(value: ZWorkflowClientOptions): ZTestEnvironmentOptions =
    copy(workflowClientOptions = value)

  def withMetricsScope(value: Scope): ZTestEnvironmentOptions =
    copy(metricsScope = Some(value))

  def withUseExternalService(value: Boolean): ZTestEnvironmentOptions =
    copy(useExternalService = Some(value))

  def withTarget(value: String): ZTestEnvironmentOptions =
    copy(target = Some(value))

  /** Allows to specify options directly on the java SDK's [[TestEnvironmentOptions]]. Use it in case an appropriate
    * `withXXX` method is missing
    *
    * @note
    *   the options specified via this method take precedence over those specified via other methods.
    */
  def transformJavaOptions(
    f: TestEnvironmentOptions.Builder => TestEnvironmentOptions.Builder
  ): ZTestEnvironmentOptions =
    copy(javaOptionsCustomization = f)

  def toJava: TestEnvironmentOptions = {
    val builder = TestEnvironmentOptions.newBuilder()

    builder.setWorkerFactoryOptions(workerFactoryOptions.toJava)
    builder.setWorkflowClientOptions(workflowClientOptions.toJava)
    metricsScope.foreach(builder.setMetricsScope)
    useExternalService.foreach(builder.setUseExternalService)
    target.foreach(builder.setTarget)

    javaOptionsCustomization(builder).build()
  }
}

object ZTestEnvironmentOptions {

  val make: URLayer[ZWorkerFactoryOptions with ZWorkflowClientOptions, ZTestEnvironmentOptions] =
    ZLayer.fromFunction(
      ZTestEnvironmentOptions(
        _: ZWorkerFactoryOptions,
        _: ZWorkflowClientOptions,
        metricsScope = None,
        useExternalService = None,
        target = None,
        javaOptionsCustomization = identity
      )
    )

  val default: ULayer[ZTestEnvironmentOptions] =
    ZLayer
      .make[ZTestEnvironmentOptions](
        make,
        ZWorkerFactoryOptions.make,
        ZWorkflowClientOptions.make
      )
      .orDie
}

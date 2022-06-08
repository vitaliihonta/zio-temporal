package ztemporal.testkit

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.uber.m3.tally.Scope
import io.temporal.common.converter.ByteArrayPayloadConverter
import io.temporal.common.converter.DefaultDataConverter
import io.temporal.common.converter.JacksonJsonPayloadConverter
import io.temporal.common.converter.NullPayloadConverter
import io.temporal.common.converter.ProtobufJsonPayloadConverter
import io.temporal.testing.TestEnvironmentOptions
import ztemporal.worker.ZWorkerFactoryOptions
import ztemporal.workflow.ZWorkflowClientOptions

/** Represents ZTestEnvironment options.
  * @see [[TestEnvironmentOptions]]
  */
class ZTestEnvironmentOptions private (
  workerFactoryOptions:  ZWorkerFactoryOptions,
  workflowClientOptions: ZWorkflowClientOptions,
  metricsScope:          Option[Scope],
  useExternalService:    Option[Boolean],
  target:                Option[String]) {

  override def toString: String =
    s"ZTestEnvironmentOptions(" +
      s"workerFactoryOptions=$workerFactoryOptions, " +
      s"workflowClientOptions=$workflowClientOptions, " +
      s"metricsScope=$metricsScope, " +
      s"useExternalService=$useExternalService, " +
      s"target=$target)"

  def withWorkerFactoryOptions(value: ZWorkerFactoryOptions): ZTestEnvironmentOptions =
    copy(_workerFactoryOptions = value)

  def withWorkflowClientOptions(value: ZWorkflowClientOptions): ZTestEnvironmentOptions =
    copy(_workflowClientOptions = value)

  def withMetricsScope(value: Scope): ZTestEnvironmentOptions =
    copy(_metricsScope = Some(value))

  def withUseExternalService(value: Boolean): ZTestEnvironmentOptions =
    copy(_useExternalService = Some(value))

  def withTarget(value: String): ZTestEnvironmentOptions =
    copy(_target = Some(value))

  def toJava: TestEnvironmentOptions = {
    val builder = TestEnvironmentOptions.newBuilder()

    builder.setWorkerFactoryOptions(workerFactoryOptions.toJava)
    builder.setWorkflowClientOptions(workflowClientOptions.toJava)
    metricsScope.foreach(builder.setMetricsScope)
    useExternalService.foreach(builder.setUseExternalService)
    target.foreach(builder.setTarget)

    builder.build()
  }

  private def copy(
    _workerFactoryOptions:  ZWorkerFactoryOptions = workerFactoryOptions,
    _workflowClientOptions: ZWorkflowClientOptions = workflowClientOptions,
    _metricsScope:          Option[Scope] = metricsScope,
    _useExternalService:    Option[Boolean] = useExternalService,
    _target:                Option[String] = target
  ): ZTestEnvironmentOptions =
    new ZTestEnvironmentOptions(
      _workerFactoryOptions,
      _workflowClientOptions,
      _metricsScope,
      _useExternalService,
      _target
    )
}

object ZTestEnvironmentOptions {

  val default: ZTestEnvironmentOptions = new ZTestEnvironmentOptions(
    workerFactoryOptions = ZWorkerFactoryOptions.default,
    workflowClientOptions = ZWorkflowClientOptions.default.withDataConverter(
      new DefaultDataConverter(
        // order matters!
        Seq(
          new NullPayloadConverter(),
          new ByteArrayPayloadConverter(),
          new ProtobufJsonPayloadConverter(),
          new JacksonJsonPayloadConverter(
            JsonMapper
              .builder()
              .addModule(DefaultScalaModule)
              .build()
          )
        ): _*
      )
    ),
    metricsScope = None,
    useExternalService = None,
    target = None
  )
}

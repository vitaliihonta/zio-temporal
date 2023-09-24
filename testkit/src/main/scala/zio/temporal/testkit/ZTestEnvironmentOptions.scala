package zio.temporal.testkit

import com.uber.m3.tally.Scope
import io.temporal.testing.TestEnvironmentOptions
import zio._
import zio.temporal.worker.ZWorkerFactoryOptions
import zio.temporal.workflow.ZWorkflowClientOptions
import java.time.Instant

/** Represents ZTestEnvironment options.
  * @see
  *   [[TestEnvironmentOptions]]
  */
final case class ZTestEnvironmentOptions private[zio] (
  workerFactoryOptions:                 ZWorkerFactoryOptions,
  workflowClientOptions:                ZWorkflowClientOptions,
  metricsScope:                         Option[Scope],
  useExternalService:                   Option[Boolean],
  target:                               Option[String],
  initialTimeMillis:                    Option[Long],
  useTimeskipping:                      Option[Boolean],
  private val javaOptionsCustomization: TestEnvironmentOptions.Builder => TestEnvironmentOptions.Builder) {

  def withWorkerFactoryOptions(value: ZWorkerFactoryOptions): ZTestEnvironmentOptions =
    copy(workerFactoryOptions = value)

  def withWorkflowClientOptions(value: ZWorkflowClientOptions): ZTestEnvironmentOptions =
    copy(workflowClientOptions = value)

  /** Sets the scope to be used for metrics reporting. Optional. Default is to not report metrics.
    *
    * <p>Note: Don't mock [[Scope]] in tests! If you need to verify the metrics behavior, create a real Scope and mock,
    * stub or spy a reporter instance:<br>
    *
    * @param value
    *   the scope to be used for metrics reporting.
    */
  def withMetricsScope(value: Scope): ZTestEnvironmentOptions =
    copy(metricsScope = Some(value))

  /** Set to true in order to make test environment use external temporal service or false for in-memory test
    * implementation.
    */
  def withUseExternalService(value: Boolean): ZTestEnvironmentOptions =
    copy(useExternalService = Some(value))

  /** Optional parameter that defines an endpoint which will be used for the communication with standalone temporal
    * service. Has no effect if [[withUseExternalService]] is set to false.
    *
    * <p>Defaults to 127.0.0.1:7233
    */
  def withTarget(value: String): ZTestEnvironmentOptions =
    copy(target = Some(value))

  /** Set the initial time for the workflow virtual clock, milliseconds since epoch.
    *
    * <p>Default is current time
    */
  def withInitialTimeMillis(value: Long): ZTestEnvironmentOptions =
    copy(initialTimeMillis = Some(value))

  /** Set the initial time for the workflow virtual clock.
    *
    * <p>Default is current time
    */
  def withInitialTime(value: Instant): ZTestEnvironmentOptions =
    withInitialTimeMillis(value.toEpochMilli)

  /** Sets whether the TestWorkflowEnvironment will timeskip. If true, no actual wall-clock time will pass when a
    * workflow sleeps or sets a timer.
    *
    * <p>Default is true
    */
  def withUseTimeskipping(value: Boolean): ZTestEnvironmentOptions =
    copy(useTimeskipping = Some(value))

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
    initialTimeMillis.foreach(builder.setInitialTimeMillis)
    useTimeskipping.foreach(builder.setUseTimeskipping)

    javaOptionsCustomization(builder).build()
  }

  override def toString: String = {
    s"ZTestEnvironmentOptions(" +
      s"workerFactoryOptions=$workerFactoryOptions" +
      s", workflowClientOptions=$workflowClientOptions" +
      s", metricsScope=$metricsScope" +
      s", useExternalService=$useExternalService" +
      s", target=$target" +
      s", initialTimeMillis=$initialTimeMillis" +
      s", useTimeskipping=$useTimeskipping" +
      s")"
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
        initialTimeMillis = None,
        useTimeskipping = None,
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

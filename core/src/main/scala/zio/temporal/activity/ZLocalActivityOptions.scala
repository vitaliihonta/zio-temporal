package zio.temporal.activity

import io.temporal.activity.LocalActivityOptions
import zio._
import zio.temporal.ZRetryOptions

/** Options used to configure how a local Activity is invoked. */
final case class ZLocalActivityOptions private[zio] (
  scheduleToCloseTimeout:          Option[Duration],
  startToCloseTimeout:             Option[Duration],
  scheduleToStartTimeout:          Option[Duration],
  localRetryThreshold:             Option[Duration],
  retryOptions:                    Option[ZRetryOptions],
  doNotIncludeArgumentsIntoMarker: Option[Boolean],
  private val javaOptionsCustomization: LocalActivityOptions.Builder => LocalActivityOptions.Builder) {

  /** @see
    *   [[ZLocalActivityOptions$.withScheduleToCloseTimeout]]
    */
  def withScheduleToCloseTimeout(value: Duration): ZLocalActivityOptions =
    copy(scheduleToCloseTimeout = Some(value))

  /** @see
    *   [[ZLocalActivityOptions$.withStartToCloseTimeout]]
    */
  def withStartToCloseTimeout(value: Duration): ZLocalActivityOptions =
    copy(startToCloseTimeout = Some(value))

  /** Time that the Activity Task can stay in the Worker's internal Task Queue of Local Activities until it's picked up
    * by the Local Activity Executor.
    *
    * <p>ScheduleToStartTimeout is always non-retryable. Retrying after this timeout doesn't make sense as it would just
    * put the Activity Task back into the same Task Queue.
    *
    * <p>Defaults to unlimited.
    */
  def withScheduleToStartTimeout(value: Duration): ZLocalActivityOptions =
    copy(scheduleToStartTimeout = Some(value))

  /** Maximum time to wait between retries locally, while keeping the Workflow Task open via a Heartbeat. If the delay
    * between the attempts becomes larger that this threshold, a Workflow Timer will be scheduled. Default value is
    * Workflow Task Timeout multiplied by 3.
    */
  def withLocalRetryThreshold(value: Duration): ZLocalActivityOptions =
    copy(localRetryThreshold = Some(value))

  /** [[ZRetryOptions]] that define how an Activity is retried in case of failure.
    *
    * <p>If both [[scheduleToStartTimeout]] and [[ZRetryOptions.maximumAttempts]] are not set, the Activity will not be
    * retried.
    *
    * <p>To ensure zero retries, set [[ZRetryOptions.maximumAttempts]] to 1.
    *
    * @see
    *   [[LocalActivityOptions.Builder]]
    */
  def withRetryOptions(value: ZRetryOptions): ZLocalActivityOptions =
    copy(retryOptions = Some(value))

  /** When set to true, the serialized arguments of the local Activity are not included in the Marker Event that stores
    * the local Activity's invocation result. The serialized arguments are included only for human troubleshooting as
    * they are never read by the SDK code. In some cases, it is better to not include them to reduce the history size.
    * The default value is set to false.
    */
  def withDoNotIncludeArgumentsIntoMarker(value: Boolean): ZLocalActivityOptions =
    copy(doNotIncludeArgumentsIntoMarker = Some(value))

  /** Allows to specify options directly on the java SDK's [[LocalActivityOptions]]. Use it in case an appropriate
    * `withXXX` method is missing
    *
    * @note
    *   the options specified via this method take precedence over those specified via other methods.
    */
  def transformJavaOptions(
    f: LocalActivityOptions.Builder => LocalActivityOptions.Builder
  ): ZLocalActivityOptions = copy(javaOptionsCustomization = f)

  /** Convert to Java SDK's [[LocalActivityOptions]]
    */
  def toJava: LocalActivityOptions = {
    val builder = LocalActivityOptions.newBuilder()

    scheduleToCloseTimeout.foreach(builder.setScheduleToCloseTimeout)
    startToCloseTimeout.foreach(builder.setStartToCloseTimeout)
    scheduleToStartTimeout.foreach(builder.setScheduleToStartTimeout)
    localRetryThreshold.foreach(builder.setLocalRetryThreshold)
    retryOptions.foreach(o => builder.setRetryOptions(o.toJava))
    doNotIncludeArgumentsIntoMarker.foreach(builder.setDoNotIncludeArgumentsIntoMarker)

    javaOptionsCustomization(builder).build()
  }
}

object ZLocalActivityOptions {

  /** Maximum time of a single Activity attempt.
    *
    * <p>If [[withScheduleToCloseTimeout]] is not provided, then this timeout is required.
    */
  def withStartToCloseTimeout(value: Duration): ZLocalActivityOptions =
    new ZLocalActivityOptions(
      scheduleToCloseTimeout = None,
      startToCloseTimeout = Some(value),
      scheduleToStartTimeout = None,
      localRetryThreshold = None,
      retryOptions = None,
      doNotIncludeArgumentsIntoMarker = None,
      javaOptionsCustomization = identity
    )

  /** Total time that a workflow is willing to wait for an Activity to complete.
    *
    * <p>ScheduleToCloseTimeout limits the total time of an Activity's execution including retries (use
    * [[withStartToCloseTimeout]] to limit the time of a single attempt).
    *
    * <p>Either this option or [[withStartToCloseTimeout]] is required.
    *
    * <p>Defaults to unlimited.
    */
  def withScheduleToCloseTimeout(value: Duration): ZLocalActivityOptions =
    new ZLocalActivityOptions(
      scheduleToCloseTimeout = Some(value),
      startToCloseTimeout = None,
      scheduleToStartTimeout = None,
      localRetryThreshold = None,
      retryOptions = None,
      doNotIncludeArgumentsIntoMarker = None,
      javaOptionsCustomization = identity
    )
}

package zio.temporal

import zio._
import io.temporal.common.RetryOptions

/** Represents temporal retry options
  *
  * @see
  *   [[RetryOptions]]
  */
final case class ZRetryOptions private[zio] (
  maximumAttempts:    Option[Int],
  initialInterval:    Option[Duration],
  backoffCoefficient: Option[Double],
  maximumInterval:    Option[Duration],
  doNotRetry:         Seq[String],
  private val javaOptionsCustomization: RetryOptions.Builder => RetryOptions.Builder) {

  /** When exceeded the amount of attempts, stop. Even if expiration time is not reached. Default is unlimited.
    */
  def withMaximumAttempts(attempts: Int): ZRetryOptions =
    copy(maximumAttempts = Some(attempts))

  /** Interval of the first retry. If coefficient is 1.0 then it is used for all retries. Required.
    */
  def withInitialInterval(interval: Duration): ZRetryOptions =
    copy(initialInterval = Some(interval))

  /** Coefficient used to calculate the next retry interval. The next retry interval is previous interval multiplied by
    * this coefficient. Must be 1 or larger. Default is 2.0.
    */
  def withBackoffCoefficient(backoffCoefficient: Double): ZRetryOptions =
    copy(backoffCoefficient = Some(backoffCoefficient))

  /** Maximum interval between retries. Exponential backoff leads to interval increase. This value is the cap of the
    * increase. Default is 100x of initial interval. Can't be less than [[initialInterval]]
    */
  def withMaximumInterval(maximumInterval: Duration): ZRetryOptions =
    copy(maximumInterval = Some(maximumInterval))

  /** List of application failures types to not retry.
    *
    * @see
    *   [[RetryOptions.Builder#setDoNotRetry]]
    */
  def withDoNotRetry(types: String*): ZRetryOptions =
    copy(doNotRetry = types)

  /** Allows to specify options directly on the java SDK's [[RetryOptions]]. Use it in case an appropriate `withXXX`
    * method is missing
    *
    * @note
    *   the options specified via this method take precedence over those specified via other methods.
    */
  def transformJavaOptions(
    f: RetryOptions.Builder => RetryOptions.Builder
  ): ZRetryOptions =
    copy(javaOptionsCustomization = f)

  def toJava: RetryOptions = {
    val builder = RetryOptions
      .newBuilder()
      .setDoNotRetry(doNotRetry: _*)

    maximumAttempts.foreach(builder.setMaximumAttempts)

    initialInterval
      .foreach(initialInterval => builder.setInitialInterval(initialInterval.asJava))

    backoffCoefficient.foreach(builder.setBackoffCoefficient)

    maximumInterval
      .foreach(maximumInterval => builder.setMaximumInterval(maximumInterval.asJava))

    javaOptionsCustomization(builder).build()
  }

  override def toString: String = {
    s"ZRetryOptions(" +
      s"maximumAttempts=$maximumAttempts" +
      s", initialInterval=$initialInterval" +
      s", backoffCoefficient=$backoffCoefficient" +
      s", maximumInterval=$maximumInterval" +
      s", doNotRetry=$doNotRetry" +
      s")"
  }
}

object ZRetryOptions {

  /** Default retry options (with no retry)
    */
  val default: ZRetryOptions = new ZRetryOptions(
    maximumAttempts = None,
    initialInterval = None,
    backoffCoefficient = None,
    maximumInterval = None,
    doNotRetry = Seq.empty,
    javaOptionsCustomization = identity
  )

  /** Creates retry options based on Java SDK's [[RetryOptions]]
    */
  def fromJava(retryOptions: RetryOptions): ZRetryOptions =
    new ZRetryOptions(
      maximumAttempts = Option(retryOptions.getMaximumAttempts),
      initialInterval = Option(retryOptions.getInitialInterval),
      backoffCoefficient = Option(retryOptions.getBackoffCoefficient),
      maximumInterval = Option(retryOptions.getMaximumInterval),
      doNotRetry = Option(retryOptions.getDoNotRetry).toList.flatten,
      javaOptionsCustomization = identity
    )
}

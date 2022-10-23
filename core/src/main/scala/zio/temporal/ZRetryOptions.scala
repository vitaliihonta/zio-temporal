package zio.temporal

import zio._
import io.temporal.common.RetryOptions

/** Represents temporal retry options
  *
  * @see
  *   [[RetryOptions]]
  */
case class ZRetryOptions private[zio] (
  maximumAttempts:                      Option[Int],
  initialInterval:                      Option[Duration],
  backoffCoefficient:                   Option[Double],
  maximumInterval:                      Option[Duration],
  doNotRetry:                           Seq[String],
  private val javaOptionsCustomization: RetryOptions.Builder => RetryOptions.Builder) {

  def withMaximumAttempts(attempts: Int): ZRetryOptions =
    copy(maximumAttempts = Some(attempts))

  def withInitialInterval(interval: Duration): ZRetryOptions =
    copy(initialInterval = Some(interval))

  def withBackoffCoefficient(backoffCoefficient: Double): ZRetryOptions =
    copy(backoffCoefficient = Some(backoffCoefficient))

  def withMaximumInterval(maximumInterval: Duration): ZRetryOptions =
    copy(maximumInterval = Some(maximumInterval))

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

    maximumAttempts
      .foreach(maximumAttempts => builder.setMaximumAttempts(maximumAttempts))

    initialInterval
      .foreach(initialInterval => builder.setInitialInterval(initialInterval.asJava))

    backoffCoefficient
      .foreach(backoffCoefficient => builder.setBackoffCoefficient(backoffCoefficient))

    maximumInterval
      .foreach(maximumInterval => builder.setMaximumInterval(maximumInterval.asJava))

    javaOptionsCustomization(builder).build()
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
}

package zio.temporal

import io.temporal.common.RetryOptions

import scala.compat.java8.DurationConverters._
import scala.concurrent.duration.FiniteDuration

/** Represents temporal retry options
  *
  * @see
  *   [[RetryOptions]]
  */
class ZRetryOptions private[zio] (
  val maximumAttempts:    Option[Int],
  val initialInterval:    Option[FiniteDuration],
  val backoffCoefficient: Option[Double],
  val maximumInterval:    Option[FiniteDuration],
  val doNotRetry:         Seq[String]) {

  override def toString: String =
    s"ZRetryOptions(" +
      s"maximumAttempts=$maximumAttempts, " +
      s"initialInterval=$initialInterval, " +
      s"backoffCoefficient=$backoffCoefficient, " +
      s"maximumInterval=$maximumInterval, " +
      s"doNotRetry=$doNotRetry)"

  def withMaximumAttempts(attempts: Int): ZRetryOptions =
    copy(_maximumAttempts = Some(attempts))

  def withInitialInterval(interval: FiniteDuration): ZRetryOptions =
    copy(_initialInterval = Some(interval))

  def withBackoffCoefficient(backoffCoefficient: Double): ZRetryOptions =
    copy(_backoffCoefficient = Some(backoffCoefficient))

  def withMaximumInterval(maximumInterval: FiniteDuration): ZRetryOptions =
    copy(_maximumInterval = Some(maximumInterval))

  def withDoNotRetry(types: String*): ZRetryOptions =
    copy(_doNotRetry = types)

  def toJava: RetryOptions = {
    val builder = RetryOptions
      .newBuilder()
      .setDoNotRetry(doNotRetry: _*)

    maximumAttempts
      .foreach(maximumAttempts => builder.setMaximumAttempts(maximumAttempts))

    initialInterval
      .foreach(initialInterval => builder.setInitialInterval(initialInterval.toJava))

    backoffCoefficient
      .foreach(backoffCoefficient => builder.setBackoffCoefficient(backoffCoefficient))

    maximumInterval
      .foreach(maximumInterval => builder.setMaximumInterval(maximumInterval.toJava))

    builder.build()
  }

  private def copy(
    _maximumAttempts:    Option[Int] = maximumAttempts,
    _initialInterval:    Option[FiniteDuration] = initialInterval,
    _backoffCoefficient: Option[Double] = backoffCoefficient,
    _maximumInterval:    Option[FiniteDuration] = maximumInterval,
    _doNotRetry:         Seq[String] = doNotRetry
  ): ZRetryOptions =
    new ZRetryOptions(_maximumAttempts, _initialInterval, _backoffCoefficient, _maximumInterval, _doNotRetry)

}

object ZRetryOptions {

  /** Default retry options (with no retry)
    */
  val default: ZRetryOptions = new ZRetryOptions(
    maximumAttempts = None,
    initialInterval = None,
    backoffCoefficient = None,
    maximumInterval = None,
    doNotRetry = Seq.empty
  )
}

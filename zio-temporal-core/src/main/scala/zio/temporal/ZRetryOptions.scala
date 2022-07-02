package zio.temporal

import zio._
import io.temporal.common.RetryOptions

/** Represents temporal retry options
  *
  * @see
  *   [[RetryOptions]]
  */
class ZRetryOptions private[zio] (
  val maximumAttempts:    Option[Int],
  val initialInterval:    Option[Duration],
  val backoffCoefficient: Option[Double],
  val maximumInterval:    Option[Duration],
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

  def withInitialInterval(interval: Duration): ZRetryOptions =
    copy(_initialInterval = Some(interval))

  def withBackoffCoefficient(backoffCoefficient: Double): ZRetryOptions =
    copy(_backoffCoefficient = Some(backoffCoefficient))

  def withMaximumInterval(maximumInterval: Duration): ZRetryOptions =
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
      .foreach(initialInterval => builder.setInitialInterval(initialInterval.asJava))

    backoffCoefficient
      .foreach(backoffCoefficient => builder.setBackoffCoefficient(backoffCoefficient))

    maximumInterval
      .foreach(maximumInterval => builder.setMaximumInterval(maximumInterval.asJava))

    builder.build()
  }

  private def copy(
    _maximumAttempts:    Option[Int] = maximumAttempts,
    _initialInterval:    Option[Duration] = initialInterval,
    _backoffCoefficient: Option[Double] = backoffCoefficient,
    _maximumInterval:    Option[Duration] = maximumInterval,
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

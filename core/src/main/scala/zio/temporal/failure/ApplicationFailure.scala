package zio.temporal.failure

import io.temporal.common.converter.Values

object ApplicationFailure {

  /** @see [[io.temporal.failure.ApplicationFailure.newFailure]] */
  def newFailure(message: String, failureType: String, details: AnyRef*): io.temporal.failure.ApplicationFailure =
    io.temporal.failure.ApplicationFailure.newFailure(message, failureType, details: _*)

  /** @see [[io.temporal.failure.ApplicationFailure.newNonRetryableFailure]] */
  def newNonRetryableFailure(
    message:     String,
    failureType: String,
    details:     AnyRef*
  ): io.temporal.failure.ApplicationFailure =
    io.temporal.failure.ApplicationFailure.newNonRetryableFailure(message, failureType, details: _*)

  /** @see [[io.temporal.failure.ApplicationFailure.newFailureWithCause]] */
  def newFailureWithCause(
    message:     String,
    failureType: String,
    cause:       Throwable,
    details:     AnyRef*
  ): io.temporal.failure.ApplicationFailure =
    io.temporal.failure.ApplicationFailure.newFailureWithCause(message, failureType, cause, details: _*)

  /** @see [[io.temporal.failure.ApplicationFailure.newNonRetryableFailureWithCause]] */
  def newNonRetryableFailureWithCause(
    message:     String,
    failureType: String,
    cause:       Throwable,
    details:     AnyRef*
  ): io.temporal.failure.ApplicationFailure =
    io.temporal.failure.ApplicationFailure.newNonRetryableFailureWithCause(message, failureType, cause, details: _*)

  def unapply(failure: ApplicationFailure): Option[(String, Throwable, Values, Boolean)] =
    Some((failure.getType, failure.getCause, failure.getDetails, failure.isNonRetryable))
}

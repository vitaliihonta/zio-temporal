package ztemporal

import scala.concurrent.duration._

/** Represents options for [[ztemporal.workflow.ZWorkflowServiceStubs.awaitTermination]] method
  */
class ZAwaitTerminationOptions private[ztemporal] (val pollTimeout: FiniteDuration, val pollDelay: FiniteDuration) {

  def withPollTimeout(timeout: FiniteDuration): ZAwaitTerminationOptions =
    new ZAwaitTerminationOptions(timeout, pollDelay)

  def withPollDelay(delay: FiniteDuration): ZAwaitTerminationOptions =
    new ZAwaitTerminationOptions(pollTimeout, delay)

  override def toString: String = s"ZAwaitTerminationOptions(pollTimeout=$pollTimeout, pollDelay=$pollDelay)"
}

object ZAwaitTerminationOptions {

  /** Used by default in [[ztemporal.workflow.ZWorkflowServiceStubs.awaitTermination]]. Configured to avoid redundant
    * polling in production.
    */
  val default: ZAwaitTerminationOptions = new ZAwaitTerminationOptions(
    pollTimeout = 5.seconds,
    pollDelay = 2.seconds
  )

  /** Default value for tests using ztemporal-testkit. Smaller than [[default]] to increase tests speed
    */
  val testDefault: ZAwaitTerminationOptions = new ZAwaitTerminationOptions(
    pollTimeout = 100.millis,
    pollDelay = 100.millis
  )
}

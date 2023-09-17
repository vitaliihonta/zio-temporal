package zio.temporal

import zio._

/** Represents options for [[zio.temporal.workflow.ZWorkflowServiceStubs.awaitTermination]] method
  */
final case class ZAwaitTerminationOptions private[zio] (pollTimeout: Duration, pollDelay: Duration) {

  def withPollTimeout(timeout: Duration): ZAwaitTerminationOptions =
    new ZAwaitTerminationOptions(timeout, pollDelay)

  def withPollDelay(delay: Duration): ZAwaitTerminationOptions =
    new ZAwaitTerminationOptions(pollTimeout, delay)

  override def toString: String = {
    s"ZAwaitTerminationOptions(" +
      s"pollTimeout=$pollTimeout" +
      s", pollDelay=$pollDelay" +
      s")"
  }
}

object ZAwaitTerminationOptions {

  /** Used by default in [[zio.temporal.workflow.ZWorkflowServiceStubs.awaitTermination]]. Configured to avoid redundant
    * polling in production.
    */
  val default: ZAwaitTerminationOptions = new ZAwaitTerminationOptions(
    pollTimeout = 5.seconds,
    pollDelay = 2.seconds
  )

  /** Default value for tests using zio.tempora-testkit. Smaller than [[default]] to increase tests speed
    */
  val testDefault: ZAwaitTerminationOptions = new ZAwaitTerminationOptions(
    pollTimeout = 100.millis,
    pollDelay = 100.millis
  )
}

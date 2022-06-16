package ztemporal.activity

import zio._
import io.temporal.activity.Activity
import ztemporal.ZActivityFatalError

/** Executed arbitrary effects within an activity implementation asynchronously completing the activity
  */
object ZActivity {

  /** Runs provided unexceptional effect completing this activity with the effect result.
    *
    * @tparam A
    *   effect result type
    * @param action
    *   the effect
    * @param zactivityOptions
    *   options required to run the action
    * @return
    *   result of executing the action
    */
  def run[A](action: URIO[ZEnv, A])(implicit zactivityOptions: ZActivityOptions): A = {
    val ctx       = Activity.getExecutionContext
    val taskToken = ctx.getTaskToken

    ctx.doNotCompleteOnReturn()

    zactivityOptions.runtime.unsafeRunAsync(action) {
      case Exit.Failure(cause) =>
        zactivityOptions.activityCompletionClient.completeExceptionally(
          taskToken,
          Activity.wrap(ZActivityFatalError(cause))
        )

      case Exit.Success(value) =>
        zactivityOptions.activityCompletionClient.complete[A](
          taskToken,
          value
        )
    }

    null.asInstanceOf[A]
  }

  /** Runs provided effect completing this activity with the effect result.
    *
    * @tparam E
    *   effect error type
    * @tparam A
    *   effect result type
    * @param action
    *   the effect
    * @param zactivityOptions
    *   options required to run the action
    * @return
    *   result of executing the action
    */
  def run[E, A](action: ZIO[ZEnv, E, A])(implicit zactivityOptions: ZActivityOptions): Either[E, A] = {
    val ctx       = Activity.getExecutionContext
    val taskToken = ctx.getTaskToken

    ctx.doNotCompleteOnReturn()

    zactivityOptions.runtime.unsafeRunAsync(action) {
      case Exit.Failure(cause) if cause.died | cause.failureOption.isEmpty =>
        zactivityOptions.activityCompletionClient.completeExceptionally(
          taskToken,
          Activity.wrap(ZActivityFatalError(cause))
        )

      case Exit.Failure(cause) =>
        zactivityOptions.activityCompletionClient.complete[Either[E, A]](
          taskToken,
          Left(cause.failureOption.get)
        )

      case Exit.Success(value) =>
        zactivityOptions.activityCompletionClient.complete[Either[E, A]](
          taskToken,
          Right(value)
        )
    }

    null.asInstanceOf[Either[E, A]]
  }
}

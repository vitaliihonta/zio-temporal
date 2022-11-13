package zio.temporal.activity

import io.temporal.activity.Activity
import zio.*
import zio.temporal.ZActivityFatalError
import zio.temporal.internal.ZioUnsafeFacade

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
  def run[R, A](action: URIO[R, A])(implicit zactivityOptions: ZActivityOptions[R]): A = {
    val ctx       = Activity.getExecutionContext
    val taskToken = ctx.getTaskToken

    ctx.doNotCompleteOnReturn()

    ZioUnsafeFacade.unsafeRunAsyncURIO[R, A](
      zactivityOptions.runtime,
      action
    )(
      onFailure = cause =>
        zactivityOptions.activityCompletionClient.completeExceptionally(
          taskToken,
          wrapCauseIntoException(cause)
        ),
      onSuccess = value =>
        zactivityOptions.activityCompletionClient.complete[A](
          taskToken,
          value
        )
    )

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
  def run[R, E, A](action: ZIO[R, E, A])(implicit zactivityOptions: ZActivityOptions[R]): Either[E, A] = {
    val ctx       = Activity.getExecutionContext
    val taskToken = ctx.getTaskToken

    ctx.doNotCompleteOnReturn()

    ZioUnsafeFacade.unsafeRunAsyncZIO[R, E, A](
      zactivityOptions.runtime,
      action
    )(
      onDie = cause =>
        zactivityOptions.activityCompletionClient.completeExceptionally(
          taskToken,
          wrapCauseIntoException(cause)
        ),
      onFailure = error =>
        zactivityOptions.activityCompletionClient.complete[Either[E, A]](
          taskToken,
          Left(error)
        ),
      onSuccess = value =>
        zactivityOptions.activityCompletionClient.complete[Either[E, A]](
          taskToken,
          Right(value)
        )
    )

    null.asInstanceOf[Either[E, A]]
  }

  private def wrapCauseIntoException(cause: Cause[_]): Exception =
    cause.defects match {
      /*Propagate temporal's non-retryable exceptions*/
      case (head: Exception) :: _ => head
      case _                      => Activity.wrap(ZActivityFatalError(cause))
    }
}

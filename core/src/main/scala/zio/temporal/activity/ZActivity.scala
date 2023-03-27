package zio.temporal.activity

import io.temporal.activity.Activity
import zio.*
import zio.temporal.internal.ZioUnsafeFacade

/** Executed arbitrary effects within an activity implementation asynchronously completing the activity
  */
object ZActivity {

  /** Runs provided effect completing this activity with the effect result.
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
  def run[R, A](action: RIO[R, A])(implicit zactivityOptions: ZActivityOptions[R]): A =
    runImpl(action)(Activity.wrap)

  /** Runs provided effect completing this activity with the effect result.
    *
    * @tparam E
    *   effect error type
    *
    * @tparam A
    *   effect result type
    * @param action
    *   the effect
    * @param zactivityOptions
    *   options required to run the action
    * @param toApplicationFailure
    *   a converter from a typed error into [[io.temporal.failure.ApplicationFailure]]
    * @return
    *   result of executing the action
    */
  def run[R, E, A](
    action:                    ZIO[R, E, A]
  )(implicit zactivityOptions: ZActivityOptions[R],
    toApplicationFailure:      ToApplicationFailure[E]
  ): A =
    runImpl(action)(toApplicationFailure.wrap)

  private def runImpl[R, E, A](
    action:                    ZIO[R, E, A]
  )(convertError:              E => Exception
  )(implicit zactivityOptions: ZActivityOptions[R]
  ): A = {
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
          Activity.wrap(cause)
        ),
      onFailure = error =>
        zactivityOptions.activityCompletionClient.completeExceptionally(
          taskToken,
          convertError(error)
        ),
      onSuccess = value =>
        zactivityOptions.activityCompletionClient.complete(
          taskToken,
          value
        )
    )

    null.asInstanceOf[A]
  }
}

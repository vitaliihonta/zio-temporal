package zio.temporal.activity

import io.temporal.activity.Activity
import io.temporal.client.ActivityCompletionException
import zio.*
import zio.temporal.internal.ZioUnsafeFacade

object ZActivity {

  /** Use this to rethrow a checked exception from an Activity Execution instead of adding the exception to a method
    * signature.
    *
    * @return
    *   Never returns; always throws. Throws original exception if e is [[RuntimeException]] or [[Error]].
    */
  def wrap(e: Throwable): RuntimeException =
    Activity.wrap(e)

  /** Can be used to get information about an Activity Execution and to invoke Heartbeats. This static method relies on
    * a thread-local variable and works only in the original Activity Execution thread.
    */
  def executionContext =
    new ZActivityExecutionContext(Activity.getExecutionContext)

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
    runImpl(action)(wrap)

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
      onDie = {
        // don't need to handle it
        case _: ActivityCompletionException =>
        case cause =>
          zactivityOptions.activityCompletionClient.completeExceptionally(
            taskToken,
            wrap(cause)
          )
      },
      onFailure = {
        // don't need to handle it
        case _: ActivityCompletionException =>
        case error =>
          zactivityOptions.activityCompletionClient.completeExceptionally(
            taskToken,
            convertError(error)
          )
      },
      onSuccess = value =>
        zactivityOptions.activityCompletionClient.complete(
          taskToken,
          value
        )
    )

    null.asInstanceOf[A]
  }
}

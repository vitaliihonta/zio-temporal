package zio.temporal.activity

import io.temporal.activity.Activity
import zio._
import zio.temporal.ZActivityFatalError

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

    Unsafe.unsafe { implicit unsafe: Unsafe =>
      val fiber = zactivityOptions.runtime.unsafe.fork(action)
      fiber.unsafe.addObserver {
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
  def run[R, E, A](action: ZIO[R, E, A])(implicit zactivityOptions: ZActivityOptions[R]): Either[E, A] = try {
    println(s"Running ZIO async")
    val ctx       = Activity.getExecutionContext
    val taskToken = ctx.getTaskToken

    ctx.doNotCompleteOnReturn()

    println(s"Invoking unsafe run")
    Unsafe.unsafe { implicit unsafe: Unsafe =>
      println("Running fiber...")
      val fiber = zactivityOptions.runtime.unsafe.fork(action)
      println("Forked fiber")

      fiber.unsafe.addObserver {
        case Exit.Failure(cause) if cause.dieOption.nonEmpty | cause.failureOption.isEmpty =>
          println(s"Activity died with fatal error $cause")
          zactivityOptions.activityCompletionClient.completeExceptionally(
            taskToken,
            Activity.wrap(ZActivityFatalError(cause))
          )

        case Exit.Failure(cause) =>
          println(s"Activity failed with error $cause")
          zactivityOptions.activityCompletionClient.complete[Either[E, A]](
            taskToken,
            Left(cause.failureOption.get)
          )

        case Exit.Success(value) =>
          println(s"Activity succeeded $value")
          zactivityOptions.activityCompletionClient.complete[Either[E, A]](
            taskToken,
            Right(value)
          )
      }
    }

    println("Returning null")
    null.asInstanceOf[Either[E, A]]
  } catch {
    case e =>
      println(s"Error while trying to run ZIO! $e")
      e.printStackTrace()
      throw e
  }
}

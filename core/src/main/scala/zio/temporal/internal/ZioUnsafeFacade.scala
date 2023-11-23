package zio.temporal.internal

import zio._

object ZioUnsafeFacade {

  def unsafeRunAsyncZIO[R, E, A](
    runtime:   Runtime[R],
    action:    ZIO[R, E, A]
  )(onDie:     Throwable => Unit,
    onFailure: E => Unit,
    onSuccess: A => Unit
  ): Unit =
    Unsafe.unsafe { implicit unsafe: Unsafe =>
      val fiber = runtime.unsafe.fork(action)
      fiber.unsafe.addObserver {
        case Exit.Failure(cause) =>
          cause.failureOrCause.fold(
            onFailure,
            _.find { case Cause.Die(t, _) => t }.map(onDie).getOrElse(onDie(new InterruptedException()))
          )
        case Exit.Success(value) => onSuccess(value)
      }
      runtime.unsafe.fork(fiber.interrupt)
    }

  def unsafeRunZIO[R, E, A](
    runtime:       Runtime[R],
    action:        ZIO[R, E, A],
    convertError:  E => Exception,
    convertDefect: Throwable => Exception
  ): A = {
    Unsafe.unsafe { implicit unsafe: Unsafe =>
      // Handle defects to avoid noisy error logs
      val errorsHandled: ZIO[R, Exception, A] = action
        .mapError(convertError)
        .catchAllDefect(defect => ZIO.fail(convertDefect(defect)))

      runtime.unsafe
        .run(errorsHandled)
        .getOrThrow()
    }
  }
}

package zio.temporal.internal

import zio.*

object ZioUnsafeFacade {

  def unsafeRunAsyncZIO[R, E, A](
    runtime:   Runtime[R],
    action:    ZIO[R, E, A]
  )(onDie:     Throwable => Unit,
    onFailure: E => Unit,
    onSuccess: A => Unit
  ): Unit =
    Unsafe.unsafe { implicit unsafe: Unsafe =>
      // Handle defects to avoid noisy error logs
      val errorsHandled: ZIO[R, Either[Throwable, E], A] = action
        .mapError(Right(_))
        .catchAllDefect(defect => ZIO.fail(Left(defect)))

      val fiber = runtime.unsafe.fork(errorsHandled)
      fiber.unsafe.addObserver {
        case Exit.Failure(cause) =>
          cause.failureOption.get match {
            case Left(defect)   => onDie(defect)
            case Right(failure) => onFailure(failure)
          }

        case Exit.Success(value) => onSuccess(value)
      }
    }
}

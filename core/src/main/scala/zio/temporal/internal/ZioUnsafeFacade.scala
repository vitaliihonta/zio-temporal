package zio.temporal.internal

import zio.*

object ZioUnsafeFacade {
  def unsafeRunAsyncURIO[R, A](
    runtime:   Runtime[R],
    action:    URIO[R, A]
  )(onFailure: Cause[Nothing] => Unit,
    onSuccess: A => Unit
  ): Unit =
    Unsafe.unsafe { implicit unsafe: Unsafe =>
      val fiber = runtime.unsafe.fork(action)
      fiber.unsafe.addObserver {
        case Exit.Failure(cause) => onFailure(cause)
        case Exit.Success(value) => onSuccess(value)
      }
    }

  def unsafeRunAsyncZIO[R, E, A](
    runtime:   Runtime[R],
    action:    ZIO[R, E, A]
  )(onDie:     Cause[E] => Unit,
    onFailure: E => Unit,
    onSuccess: A => Unit
  ): Unit =
    Unsafe.unsafe { implicit unsafe: Unsafe =>
      val fiber = runtime.unsafe.fork(action)
      fiber.unsafe.addObserver {
        case Exit.Failure(cause) if cause.dieOption.nonEmpty | cause.failureOption.isEmpty => onDie(cause)
        case Exit.Failure(cause) => onFailure(cause.failureOption.get)
        case Exit.Success(value) => onSuccess(value)
      }
    }
}

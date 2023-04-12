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
      val fiber = runtime.unsafe.fork(action)
      fiber.unsafe.addObserver {
        case Exit.Failure(cause) if cause.dieOption.nonEmpty => onDie(cause.dieOption.get)
        case Exit.Failure(cause)                             => onFailure(cause.failureOption.get)
        case Exit.Success(value)                             => onSuccess(value)
      }
    }
}

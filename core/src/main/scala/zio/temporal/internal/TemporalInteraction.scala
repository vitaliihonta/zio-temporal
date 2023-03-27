package zio.temporal.internal

import io.temporal.client.WorkflowException
import zio.ZIO
import zio.temporal.TemporalIO
import zio.temporal.internalApi

import java.util.concurrent.CompletableFuture

@internalApi
object TemporalInteraction {

  def from[A](thunk: => A): TemporalIO[A] =
    ZIO
      .attempt(thunk)
      .refineToOrDie[WorkflowException]

  def fromFuture[A](future: => CompletableFuture[A]): TemporalIO[A] =
    ZIO
      .fromFutureJava(future)
      .refineToOrDie[WorkflowException]
}

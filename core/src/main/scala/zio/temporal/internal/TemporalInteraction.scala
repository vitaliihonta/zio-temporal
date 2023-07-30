package zio.temporal.internal

import io.temporal.client.WorkflowException
import zio.ZIO
import zio.temporal.TemporalIO
import zio.temporal.internalApi

import java.util.concurrent.CompletableFuture
import scala.concurrent.TimeoutException

@internalApi
object TemporalInteraction {

  def from[A](thunk: => A): TemporalIO[A] = {
    ZIO
      .attemptBlocking(thunk)
      .refineToOrDie[WorkflowException]
  }

  def fromFuture[A](future: => CompletableFuture[A]): TemporalIO[A] =
    ZIO
      .fromFutureJava(future)
      .refineToOrDie[WorkflowException]

  def fromFutureTimeout[A](future: => CompletableFuture[A]): TemporalIO[Option[A]] =
    ZIO
      .fromFutureJava(future)
      .map(Option(_))
      .catchSome { case _: TimeoutException =>
        ZIO.none
      }
      .refineToOrDie[WorkflowException]
}

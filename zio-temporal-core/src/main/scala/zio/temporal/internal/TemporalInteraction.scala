package zio.temporal.internal

import zio.{IO, ZIO}
import zio.temporal.{TemporalBusinessError, TemporalClientError, TemporalError, TemporalIO}

import java.util.concurrent.CompletableFuture

private[zio] object TemporalInteraction {

  def from[A](thunk: => A): TemporalIO[TemporalClientError, A] =
    ZIO
      .effect(thunk)
      .mapError(TemporalClientError)

  def fromEither[E, A](thunk: => Either[E, A]): TemporalIO[TemporalError[E], A] =
    ZIO
      .effect(thunk)
      .mapError(TemporalClientError)
      .flatMap(IO.fromEither(_).mapError(TemporalBusinessError(_)))

  def fromFuture[A](future: => CompletableFuture[A]): TemporalIO[TemporalClientError, A] =
    ZIO
      .fromFutureJava(future)
      .mapError(TemporalClientError)

  def fromFutureEither[E, A](future: => CompletableFuture[Either[E, A]]): TemporalIO[TemporalError[E], A] =
    fromFuture(future)
      .flatMap(IO.fromEither(_).mapError(TemporalBusinessError(_)))
}

package zio.temporal.internal

import zio.ZIO
import zio.temporal.TemporalBusinessError
import zio.temporal.TemporalClientError
import zio.temporal.TemporalError
import zio.temporal.TemporalIO
import zio.temporal.internalApi
import java.util.concurrent.CompletableFuture

@internalApi
object TemporalInteraction {

  def from[A](thunk: => A): TemporalIO[TemporalClientError, A] =
    ZIO.log("Interacting with temporal...") *>
      ZIO
        .attempt(thunk)
        .tapBoth(e => ZIO.log(s"Error interacting with temporal: $e"), res => ZIO.log(s"Interaction succeeded: $res"))
        .mapError(TemporalClientError)

  def fromEither[E, A](thunk: => Either[E, A]): TemporalIO[TemporalError[E], A] =
    ZIO
      .attempt(thunk)
      .mapError(TemporalClientError)
      .flatMap(ZIO.fromEither(_).mapError(TemporalBusinessError(_)))

  def fromFuture[A](future: => CompletableFuture[A]): TemporalIO[TemporalClientError, A] =
    ZIO
      .fromFutureJava(future)
      .mapError(TemporalClientError)

  def fromFutureEither[E, A](future: => CompletableFuture[Either[E, A]]): TemporalIO[TemporalError[E], A] =
    fromFuture(future)
      .flatMap(ZIO.fromEither(_).mapError(TemporalBusinessError(_)))
}

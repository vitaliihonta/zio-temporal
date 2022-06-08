package ztemporal.internal

import zio.IO
import zio.ZIO
import ztemporal.ZTemporalBusinessError
import ztemporal.ZTemporalClientError
import ztemporal.ZTemporalError
import ztemporal.ZTemporalIO

import java.util.concurrent.CompletableFuture

private[ztemporal] object TemporalInteraction {

  def from[A](thunk: => A): ZTemporalIO[ZTemporalClientError, A] =
    ZIO
      .effect(thunk)
      .mapError(ZTemporalClientError)

  def fromEither[E, A](thunk: => Either[E, A]): ZTemporalIO[ZTemporalError[E], A] =
    ZIO
      .effect(thunk)
      .mapError(ZTemporalClientError)
      .flatMap(IO.fromEither(_).mapError(ZTemporalBusinessError(_)))

  def fromFuture[A](future: => CompletableFuture[A]): ZTemporalIO[ZTemporalClientError, A] =
    ZIO
      .fromFutureJava(future)
      .mapError(ZTemporalClientError)

  def fromFutureEither[E, A](future: => CompletableFuture[Either[E, A]]): ZTemporalIO[ZTemporalError[E], A] =
    fromFuture(future)
      .flatMap(IO.fromEither(_).mapError(ZTemporalBusinessError(_)))
}

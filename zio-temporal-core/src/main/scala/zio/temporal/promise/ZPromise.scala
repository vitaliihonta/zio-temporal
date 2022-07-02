package zio.temporal.promise

import io.temporal.failure.CanceledFailure
import io.temporal.workflow.Async
import io.temporal.workflow.Promise
import zio._
import zio.temporal.ZCanceledFailure

import java.util.concurrent.TimeUnit
import scala.annotation.unchecked.uncheckedVariance
import scala.collection.mutable
import scala.concurrent.TimeoutException
import scala.jdk.CollectionConverters._
import scala.util.Try

/** Contains result of an asynchronous computation. Similar to [[zio.IO]] with the following differences:
  *
  *   1. Can be used only inside a Temporal workflow code. Use [[zio.ZIO]] and its derivatives to implement activities
  *      and workflow starting and querying code. `run` method doesn't throw [[InterruptedException]]. The only way to
  *      unblock `run` is to complete the [[ZPromise]]
  *
  * 2. [[ZPromise]] doesn't directly supports cancellation. Use [[io.temporal.workflow.CancellationScope]] to cancel and
  * handle cancellations. The pattern is that a canceled operation completes its [[ZPromise]] with
  * [[io.temporal.failure.CanceledFailure]] when canceled.
  */
sealed trait ZPromise[+E, +A] { self =>
  protected val underlying: Promise[Either[E, A]] @uncheckedVariance

  /** Blocks until the promise completes
    *
    * @return
    *   either result or error
    */
  def run: ZPromise.Result[ZPromise.NoEffects, E, A]

  def runCancellable: ZPromise.Result[ZPromise.Cancel, E, A]

  def run(timeout: Duration): ZPromise.Result[ZPromise.Timeout, E, A]

  def runCancellable(timeout: Duration): ZPromise.Result[ZPromise.Cancel with ZPromise.Timeout, E, A]

  def swap: ZPromise[A, E]

  def map[B](f: A => B): ZPromise[E, B]

  def as[B](value: B): ZPromise[E, B] =
    self.map(_ => value)

  def unit: ZPromise[E, Unit] =
    self.as(())

  def mapError[E2](f: E => E2): ZPromise[E2, A]

  def flatMap[E2 >: E, B](f: A => ZPromise[E2, B]): ZPromise[E2, B]

  def flatMapError[E2](f: E => ZPromise[Nothing, E2]): ZPromise[E2, A]

  def catchAll[E2, A0 >: A](f: E => ZPromise[E2, A0]): ZPromise[E2, A0]

  def catchSome[E0 >: E, A0 >: A](pf: PartialFunction[E, ZPromise[E0, A0]]): ZPromise[E0, A0]

  final def zipWith[E1 >: E, B, C](that: => ZPromise[E1, B])(f: (A, B) => C): ZPromise[E1, C] =
    self.flatMap(a => that.map(f(a, _)))
}

object ZPromise {
  sealed trait NoEffects
  sealed trait Cancel  extends NoEffects
  sealed trait Timeout extends NoEffects

  /** Represents [[ZPromise]] execution result
    *
    * @tparam C
    *   [[ZPromise]] effect (either [[NoEffects]] or [[Cancel]] or [[Timeout]]
    * @tparam E
    *   error type
    * @tparam A
    *   value type
    */
  sealed trait Result[-C <: NoEffects, +E, +A]
  case class Success[A](value: A)                 extends Result[NoEffects, Nothing, A]
  case class Failure[E](error: E)                 extends Result[NoEffects, E, Nothing]
  case class Cancelled(failure: ZCanceledFailure) extends Result[Cancel, Nothing, Nothing]
  case object TimedOut                            extends Result[Timeout, Nothing, Nothing]

  /** Creates successfully completed [[ZPromise]]
    * @tparam A
    *   value type
    * @param value
    *   the value
    * @return
    *   promise completed with value
    */
  def succeed[A](value: A): ZPromise[Nothing, A] = new Impl[Nothing, A](Async.function(() => Right(value)))

  /** Creates failed [[ZPromise]]
    * @tparam E
    *   error type
    * @param error
    *   the error
    * @return
    *   promise completed with error
    */
  def fail[E](error: E): ZPromise[E, Nothing] = new Impl[E, Nothing](Async.function(() => Left(error)))

  /** Suspends side effect execution within [[ZPromise]]
    * @tparam A
    *   value type
    * @tparam E
    *   error type
    * @param thunk
    *   side effect
    * @return
    *   suspended promise
    */
  def fromEither[E, A](thunk: => Either[E, A]): ZPromise[E, A] = new Impl[E, A](Async.function(() => thunk))

  /** Suspends side effect execution within [[ZPromise]]
    * @tparam A
    *   value type
    * @param thunk
    *   effectful side effect (which may throw exceptions)
    * @return
    *   suspended promise
    */
  def effect[A](thunk: => A): ZPromise[Throwable, A] =
    new Impl[Throwable, A](
      Async.function(() => Try(thunk).toEither)
    )

  /** Ensures sequence of promises are either finished successfully or failed
    *
    * @see
    *   [[Promise.allOf]]
    * @param in
    *   sequence of promises
    * @return
    *   suspended promise
    */
  def collectAll_(in: Iterable[ZPromise[Any, Any]]): ZPromise[Nothing, Unit] =
    new ZPromise.Impl[Nothing, Unit](
      Promise.allOf(in.map(_.underlying).asJava).thenApply(_ => Right(()))
    )

  /** Similar to [[zio.ZIO.foreach]] for Option
    *
    * @param in
    *   optional value
    * @param f
    *   value handler
    * @return
    *   promise with collected result, None or failure
    */
  def foreach[E, A, B](in: Option[A])(f: A => ZPromise[E, B]): ZPromise[E, Option[B]] =
    in.fold[ZPromise[E, Option[B]]](succeed(None))(f(_).map(Some(_)))

  /** Similar to [[zio.ZIO.foreach]] for collections
    *
    * @param in
    *   sequence of values
    * @param f
    *   value handler
    * @return
    *   promise with collected results or failure
    */
  def foreach[E, A, B, Collection[+Element] <: Iterable[Element]](
    in:          Collection[A]
  )(f:           A => ZPromise[E, B]
  )(implicit bf: BuildFrom[Collection[A], B, Collection[B]]
  ): ZPromise[E, Collection[B]] =
    in.foldLeft[ZPromise[E, mutable.Builder[B, Collection[B]]]](succeed(bf(in)))((io, a) => io.zipWith(f(a))(_ += _))
      .map(_.result())

  private[zio] final class Impl[E, A] private[zio] (override val underlying: Promise[Either[E, A]])
      extends ZPromise[E, A] {

    override def run: ZPromise.Result[NoEffects, E, A] =
      underlying
        .get()
        .fold[ZPromise.Result[NoEffects, E, A]](
          ZPromise.Failure(_),
          ZPromise.Success(_)
        )

    override def run(timeout: Duration): ZPromise.Result[Timeout, E, A] =
      try
        underlying
          .get(timeout.toNanos, TimeUnit.NANOSECONDS)
          .fold[ZPromise.Result[Timeout, E, A]](
            ZPromise.Failure(_),
            ZPromise.Success(_)
          )
      catch {
        case _: TimeoutException => ZPromise.TimedOut
      }

    override def runCancellable: ZPromise.Result[Cancel, E, A] =
      try
        underlying
          .cancellableGet()
          .fold[ZPromise.Result[Cancel, E, A]](
            ZPromise.Failure(_),
            ZPromise.Success(_)
          )
      catch {
        case e: CanceledFailure => ZPromise.Cancelled(new ZCanceledFailure(e))
      }

    override def runCancellable(timeout: Duration): Result[Cancel with Timeout, E, A] =
      try
        underlying
          .cancellableGet(timeout.toNanos, TimeUnit.NANOSECONDS)
          .fold[ZPromise.Result[Timeout, E, A]](
            ZPromise.Failure(_),
            ZPromise.Success(_)
          )
      catch {
        case e: CanceledFailure  => ZPromise.Cancelled(new ZCanceledFailure(e))
        case _: TimeoutException => ZPromise.TimedOut
      }

    override def swap: ZPromise[A, E] = new Impl[A, E](underlying.thenApply(_.swap))

    override def map[B](f: A => B): ZPromise[E, B] =
      new Impl[E, B](underlying.thenApply(_.map(f)))

    override def mapError[E2](f: E => E2): ZPromise[E2, A] =
      new Impl[E2, A](underlying.thenApply(_.left.map(f)))

    override def flatMap[E2 >: E, B](f: A => ZPromise[E2, B]): ZPromise[E2, B] =
      new Impl[E2, B](underlying.thenCompose { result =>
        result.fold(error => Async.function(() => Left(error)), f(_).underlying)
      })

    override def flatMapError[E2](f: E => ZPromise[Nothing, E2]): ZPromise[E2, A] =
      new Impl[E2, A](underlying.thenCompose { result =>
        result.fold[Promise[Either[E2, A]]](
          f(_).swap.underlying.asInstanceOf[Promise[Either[E2, A]]],
          ZPromise.succeed(_).underlying.asInstanceOf[Promise[Either[E2, A]]]
        )
      })

    override def catchAll[E2, A0 >: A](f: E => ZPromise[E2, A0]): ZPromise[E2, A0] =
      new Impl[E2, A0](underlying.thenCompose { result =>
        result.fold[Promise[Either[E2, A0]]](
          f(_).underlying,
          ZPromise.succeed(_).underlying.asInstanceOf[Promise[Either[E2, A0]]]
        )
      })

    override def catchSome[E0 >: E, A0 >: A](pf: PartialFunction[E, ZPromise[E0, A0]]): ZPromise[E0, A0] =
      new Impl[E0, A0](underlying.thenCompose { result =>
        result.fold[Promise[Either[E0, A0]]](
          pf.applyOrElse[E, ZPromise[E0, A0]](_, ZPromise.fail).underlying,
          ZPromise.succeed(_).underlying.asInstanceOf[Promise[Either[E0, A0]]]
        )
      })
  }

  final object Result {

    final implicit class UnexceptionalOps[A](private val self: Result[NoEffects, Nothing, A]) extends AnyVal {

      def value: A =
        self match {
          case Success(value) => value
        }
    }

    final implicit class AllEffectsOps[E, A](private val self: Result[Cancel with Timeout, E, A]) extends AnyVal {

      def foldAll[B](onTimeout: => B)(cancelled: ZCanceledFailure => B, failed: E => B, succeeded: A => B): B =
        self match {
          case Success(value)     => succeeded(value)
          case Failure(error)     => failed(error)
          case Cancelled(failure) => cancelled(failure)
          case TimedOut           => onTimeout
        }
    }

    final implicit class CancellableOps[E, A](private val self: Result[Cancel, E, A]) extends AnyVal {

      def foldCancel[B](cancelled: ZCanceledFailure => B, failed: E => B, succeeded: A => B): B =
        self match {
          case Success(value)     => succeeded(value)
          case Failure(error)     => failed(error)
          case Cancelled(failure) => cancelled(failure)
        }
    }

    final implicit class NoEffectsOps[E, A](private val self: Result[NoEffects, E, A]) extends AnyVal {

      def foldNoEffects[B](failed: E => B, succeeded: A => B): B =
        self match {
          case Success(value) => succeeded(value)
          case Failure(error) => failed(error)
        }

      def toEither: Either[E, A] = foldNoEffects[Either[E, A]](Left(_), Right(_))
    }

    final implicit class TimeoutOps[E, A](private val self: Result[Timeout, E, A]) extends AnyVal {

      def foldTimedOut[B](onTimeout: => B)(failed: E => B, succeeded: A => B): B =
        self match {
          case Success(value) => succeeded(value)
          case Failure(error) => failed(error)
          case TimedOut       => onTimeout
        }
    }
  }
}

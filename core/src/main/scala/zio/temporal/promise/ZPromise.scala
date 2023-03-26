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
  *      unblock `run` is to complete the [[ZAsync]]
  *
  * 2. [[ZAsync]] doesn't directly supports cancellation. Use [[io.temporal.workflow.CancellationScope]] to cancel and
  * handle cancellations. The pattern is that a canceled operation completes its [[ZAsync]] with
  * [[io.temporal.failure.CanceledFailure]] when canceled.
  */
sealed trait ZAsync[+E, +A] { self =>
  protected val underlying: Promise[Either[E, A]] @uncheckedVariance

  /** Blocks until the promise completes
    *
    * @return
    *   either result or error
    */
  def run: ZAsync.Result[ZAsync.NoEffects, E, A]

  def runCancellable: ZAsync.Result[ZAsync.Cancel, E, A]

  def run(timeout: Duration): ZAsync.Result[ZAsync.Timeout, E, A]

  def runCancellable(timeout: Duration): ZAsync.Result[ZAsync.Cancel with ZAsync.Timeout, E, A]

  def swap: ZAsync[A, E]

  def map[B](f: A => B): ZAsync[E, B]

  def as[B](value: B): ZAsync[E, B] =
    self.map(_ => value)

  def unit: ZAsync[E, Unit] =
    self.as(())

  def mapError[E2](f: E => E2): ZAsync[E2, A]

  def flatMap[E2 >: E, B](f: A => ZAsync[E2, B]): ZAsync[E2, B]

  def flatMapError[E2](f: E => ZAsync[Nothing, E2]): ZAsync[E2, A]

  def catchAll[E2, A0 >: A](f: E => ZAsync[E2, A0]): ZAsync[E2, A0]

  def catchSome[E0 >: E, A0 >: A](pf: PartialFunction[E, ZAsync[E0, A0]]): ZAsync[E0, A0]

  final def zipWith[E1 >: E, B, C](that: => ZAsync[E1, B])(f: (A, B) => C): ZAsync[E1, C] =
    self.flatMap(a => that.map(f(a, _)))
}

object ZAsync {
  sealed trait NoEffects
  sealed trait Cancel  extends NoEffects
  sealed trait Timeout extends NoEffects

  /** Represents [[ZAsync]] execution result
    *
    * @tparam C
    *   [[ZAsync]] effect (either [[NoEffects]] or [[Cancel]] or [[Timeout]]
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

  /** Creates successfully completed [[ZAsync]]
    *
    * @tparam A
    *   value type
    * @param value
    *   the value
    * @return
    *   promise completed with value
    */
  def succeed[A](value: A): ZAsync[Nothing, A] = new Impl[Nothing, A](Async.function(() => Right(value)))

  /** Creates failed [[ZAsync]]
    *
    * @tparam E
    *   error type
    * @param error
    *   the error
    * @return
    *   promise completed with error
    */
  def fail[E](error: E): ZAsync[E, Nothing] = new Impl[E, Nothing](Async.function(() => Left(error)))

  /** Suspends side effect execution within [[ZAsync]]
    *
    * @tparam A
    *   value type
    * @tparam E
    *   error type
    * @param thunk
    *   side effect
    * @return
    *   suspended promise
    */
  def fromEither[E, A](thunk: => Either[E, A]): ZAsync[E, A] = new Impl[E, A](Async.function(() => thunk))

  /** Suspends side effect execution within [[ZAsync]]
    *
    * @tparam A
    *   value type
    * @param thunk
    *   effectful side effect (which may throw exceptions)
    * @return
    *   suspended promise
    */
  def attempt[A](thunk: => A): ZAsync[Throwable, A] =
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
  def collectAllDiscard(in: Iterable[ZAsync[Any, Any]]): ZAsync[Nothing, Unit] =
    new ZAsync.Impl[Nothing, Unit](
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
  def foreach[E, A, B](in: Option[A])(f: A => ZAsync[E, B]): ZAsync[E, Option[B]] =
    in.fold[ZAsync[E, Option[B]]](succeed(None))(f(_).map(Some(_)))

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
  )(f:           A => ZAsync[E, B]
  )(implicit bf: BuildFrom[Collection[A], B, Collection[B]]
  ): ZAsync[E, Collection[B]] =
    in.foldLeft[ZAsync[E, mutable.Builder[B, Collection[B]]]](succeed(bf(in)))((io, a) => io.zipWith(f(a))(_ += _))
      .map(_.result())

  private[zio] final class Impl[E, A] private[zio] (override val underlying: Promise[Either[E, A]])
      extends ZAsync[E, A] {

    override def run: ZAsync.Result[NoEffects, E, A] =
      underlying
        .get()
        .fold[ZAsync.Result[NoEffects, E, A]](
          ZAsync.Failure(_),
          ZAsync.Success(_)
        )

    override def run(timeout: Duration): ZAsync.Result[Timeout, E, A] =
      try
        underlying
          .get(timeout.toNanos, TimeUnit.NANOSECONDS)
          .fold[ZAsync.Result[Timeout, E, A]](
            ZAsync.Failure(_),
            ZAsync.Success(_)
          )
      catch {
        case _: TimeoutException => ZAsync.TimedOut
      }

    override def runCancellable: ZAsync.Result[Cancel, E, A] =
      try
        underlying
          .cancellableGet()
          .fold[ZAsync.Result[Cancel, E, A]](
            ZAsync.Failure(_),
            ZAsync.Success(_)
          )
      catch {
        case e: CanceledFailure => ZAsync.Cancelled(new ZCanceledFailure(e))
      }

    override def runCancellable(timeout: Duration): Result[Cancel with Timeout, E, A] =
      try
        underlying
          .cancellableGet(timeout.toNanos, TimeUnit.NANOSECONDS)
          .fold[ZAsync.Result[Timeout, E, A]](
            ZAsync.Failure(_),
            ZAsync.Success(_)
          )
      catch {
        case e: CanceledFailure  => ZAsync.Cancelled(new ZCanceledFailure(e))
        case _: TimeoutException => ZAsync.TimedOut
      }

    override def swap: ZAsync[A, E] = new Impl[A, E](underlying.thenApply(_.swap))

    override def map[B](f: A => B): ZAsync[E, B] =
      new Impl[E, B](underlying.thenApply(_.map(f)))

    override def mapError[E2](f: E => E2): ZAsync[E2, A] =
      new Impl[E2, A](underlying.thenApply(_.left.map(f)))

    override def flatMap[E2 >: E, B](f: A => ZAsync[E2, B]): ZAsync[E2, B] =
      new Impl[E2, B](underlying.thenCompose { result =>
        result.fold(error => Async.function(() => Left(error)), f(_).underlying)
      })

    override def flatMapError[E2](f: E => ZAsync[Nothing, E2]): ZAsync[E2, A] =
      new Impl[E2, A](underlying.thenCompose { result =>
        result.fold[Promise[Either[E2, A]]](
          f(_).swap.underlying.asInstanceOf[Promise[Either[E2, A]]],
          ZAsync.succeed(_).underlying.asInstanceOf[Promise[Either[E2, A]]]
        )
      })

    override def catchAll[E2, A0 >: A](f: E => ZAsync[E2, A0]): ZAsync[E2, A0] =
      new Impl[E2, A0](underlying.thenCompose { result =>
        result.fold[Promise[Either[E2, A0]]](
          f(_).underlying,
          ZAsync.succeed(_).underlying.asInstanceOf[Promise[Either[E2, A0]]]
        )
      })

    override def catchSome[E0 >: E, A0 >: A](pf: PartialFunction[E, ZAsync[E0, A0]]): ZAsync[E0, A0] =
      new Impl[E0, A0](underlying.thenCompose { result =>
        result.fold[Promise[Either[E0, A0]]](
          pf.applyOrElse[E, ZAsync[E0, A0]](_, ZAsync.fail).underlying,
          ZAsync.succeed(_).underlying.asInstanceOf[Promise[Either[E0, A0]]]
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

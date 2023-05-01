package zio.temporal.workflow

import io.temporal.failure.CanceledFailure
import io.temporal.workflow.{Async, Functions, Promise}
import zio.*
import zio.temporal.internal.TemporalWorkflowFacade.FunctionConverters.*
import java.util.concurrent.TimeUnit
import scala.annotation.unchecked.uncheckedVariance
import scala.collection.mutable
import scala.concurrent.TimeoutException
import scala.jdk.CollectionConverters.*
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
  *
  * 3. Unlike [[zio.IO]] (that is '''lazy'''), [[ZAsync]] is '''strict'''. Whenever a [[ZAsync]] is created, The thunk
  * is immediately started
  */
sealed trait ZAsync[+A] { self =>
  protected val underlying: Promise[A] @uncheckedVariance

  /** Blocks until the promise completes
    *
    * @return
    *   either result or error
    */
  def run: ZAsync.Result[ZAsync.NoEffects, A]

  def runCancellable: ZAsync.Result[ZAsync.Cancel, A]

  def run(timeout: Duration): ZAsync.Result[ZAsync.Timeout, A]

  def runCancellable(timeout: Duration): ZAsync.Result[ZAsync.Cancel with ZAsync.Timeout, A]

  def map[B](f: A => B): ZAsync[B]

  def as[B](value: B): ZAsync[B] =
    self.map(_ => value)

  def unit: ZAsync[Unit] =
    self.as(())

  def flatMap[B](f: A => ZAsync[B]): ZAsync[B]

  def catchAll[A0 >: A](f: Throwable => ZAsync[A0]): ZAsync[A0]

  def catchSome[A0 >: A](pf: PartialFunction[Throwable, ZAsync[A0]]): ZAsync[A0]

  final def zipWith[B, C](that: => ZAsync[B])(f: (A, B) => C): ZAsync[C] =
    self.flatMap(a => that.map(f(a, _)))

  /** The only difference with `zipWith` is that `that` is by-value. Therefore, left ZAsync is already started
    */
  final def zipPar[B, C](that: ZAsync[B])(f: (A, B) => C): ZAsync[C] =
    self.flatMap(a => that.map(f(a, _)))

  /** Return None if error occurred */
  final def option: ZAsync[Option[A]] =
    self.map(Some(_)).catchAll(_ => ZAsync.succeed(None))

  /** Ignore any errors */
  final def ignore: ZAsync[Unit] =
    self.unit.catchAll(_ => ZAsync.unit)

  final def tap(f: A => Unit): ZAsync[A] =
    self.map { a =>
      f(a)
      a
    }

  final def tapError(f: Throwable => Unit): ZAsync[A] =
    self.catchAll { e =>
      f(e)
      ZAsync.fail(e)
    }
}

object ZAsync {
  sealed trait NoEffects
  sealed trait Cancel  extends NoEffects
  sealed trait Timeout extends NoEffects

  /** Represents [[ZAsync]] execution result
    *
    * @tparam C
    *   [[ZAsync]] effect (either [[NoEffects]] or [[Cancel]] or [[Timeout]]
    * @tparam A
    *   value type
    */
  sealed trait Result[-C <: NoEffects, +A] {
    def getOrThrow: A
  }
  case class Success[A](value: A) extends Result[NoEffects, A] {
    override def getOrThrow: A = value
  }
  case class Failure(error: Throwable) extends Result[NoEffects, Nothing] {
    override def getOrThrow: Nothing = throw error
  }
  case class Cancelled(failure: CanceledFailure) extends Result[Cancel, Nothing] {
    override def getOrThrow: Nothing = throw failure
  }
  case class TimedOut(failure: TimeoutException) extends Result[Timeout, Nothing] {
    override def getOrThrow: Nothing = throw failure
  }

  /** Wraps Temporal's [[Promise]] into [[ZAsync]]
    *
    * @tparam A
    *   value type
    * @param promise
    *   the promise
    * @return
    *   promise wrapped
    */
  def fromJava[A](promise: Promise[A]): ZAsync[A] =
    new Impl[A](promise)

  /** Creates successfully completed [[ZAsync]]
    *
    * @tparam A
    *   value type
    * @param value
    *   the value
    * @return
    *   promise completed with value
    */
  def succeed[A](value: A): ZAsync[A] =
    new Impl[A](Async.function(() => value))

  def unit: ZAsync[Unit] =
    ZAsync.succeed(())

  /** Creates failed [[ZAsync]]
    *
    * @param error
    *   the error
    * @return
    *   promise completed with error
    */
  def fail(error: Throwable): ZAsync[Nothing] =
    new Impl[Nothing](Async.function[Nothing](() => throw error))

  /** Suspends side effect execution within [[ZAsync]]
    *
    * @tparam A
    *   value type
    * @param thunk
    *   side effect
    * @return
    *   suspended promise
    */
  def fromEither[A](thunk: => Either[Throwable, A]): ZAsync[A] =
    new Impl[A](
      Async.function[A](() =>
        thunk match {
          case Left(error)  => throw error
          case Right(value) => value
        }
      )
    )

  /** Suspends side effect execution within [[ZAsync]]
    *
    * @tparam A
    *   value type
    * @param thunk
    *   effectful side effect (which may throw exceptions)
    * @return
    *   suspended promise
    */
  def attempt[A](thunk: => A): ZAsync[A] =
    new Impl[A](Async.function(() => thunk))

  /** Returns [[ZAsync]] that becomes completed when all promises in the collection are completed. A single promise
    * failure causes resulting promise to deliver the failure immediately.
    *
    * @param in
    *   async computations to wait for.
    * @return
    *   [[ZAsync]] that is completed with null when all the argument promises become completed.
    */
  def collectAllDiscard(in: Iterable[ZAsync[Any]]): ZAsync[Unit] =
    new ZAsync.Impl[Unit](
      Promise.allOf(in.map(_.underlying).asJava).thenApply(_ => ())
    )

  /** Returns [[ZAsync]] that becomes completed when any of the arguments is completed. If it completes exceptionally
    * then result is also completes exceptionally.
    */
  def raceFirst[A](in: Iterable[ZAsync[A]]): ZAsync[A] =
    new ZAsync.Impl[A](
      Promise.anyOf(in.map(_.underlying).asJava)
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
  def foreach[A, B](in: Option[A])(f: A => ZAsync[B]): ZAsync[Option[B]] =
    in.fold[ZAsync[Option[B]]](succeed(None))(f(_).map(Some(_)))

  /** Similar to [[zio.ZIO.foreach]] for collections
    *
    * @param in
    *   sequence of values
    * @param f
    *   value handler
    * @return
    *   promise with collected results or failure
    */
  def foreach[A, B, Collection[+Element] <: Iterable[Element]](
    in:          Collection[A]
  )(f:           A => ZAsync[B]
  )(implicit bf: BuildFrom[Collection[A], B, Collection[B]]
  ): ZAsync[Collection[B]] =
    in.foldLeft[ZAsync[mutable.Builder[B, Collection[B]]]](succeed(bf(in)))((io, a) => io.zipWith(f(a))(_ += _))
      .map(_.result())

  /** Similar to [[zio.ZIO.foreachPar]] for collections
    *
    * @param in
    *   sequence of values
    * @param f
    *   value handler
    * @return
    *   promise with collected results or failure
    */
  def foreachPar[A, B, Collection[+Element] <: Iterable[Element]](
    in: Collection[A]
  )(f:  A => ZAsync[B]
  )(implicit
    bf: BuildFrom[Collection[A], B, Collection[B]]
  ): ZAsync[Collection[B]] = {
    val started = in.map(f)
    ZAsync
      .collectAllDiscard(started)
      .map { _ =>
        (bf.newBuilder(in) ++= started.map(_.run.getOrThrow)).result()
      }
  }

  /** Similar to [[zio.ZIO.foreachParDiscard]] for collections
    *
    * @param in
    *   sequence of values
    * @param f
    *   value handler
    * @return
    *   promise which succeeds if all [[ZAsync]] succeed
    */
  def foreachParDiscard[A](
    in: Iterable[A]
  )(f:  A => ZAsync[Any]
  ): ZAsync[Unit] = {
    val started = in.map(f)
    ZAsync.collectAllDiscard(started)
  }

  /** Similar to [[zio.ZIO.collectAll]] for collections
    *
    * @param in
    *   sequence of [[ZAsync]]
    * @return
    *   promise with collected results or failure
    */
  def collectAll[A, Collection[+Element] <: Iterable[Element]](
    in: Collection[ZAsync[A]]
  )(implicit
    bf: BuildFrom[Collection[ZAsync[A]], A, Collection[A]]
  ): ZAsync[Collection[A]] =
    ZAsync
      .collectAllDiscard(in)
      .map { _ =>
        (bf.newBuilder(in) ++= in.map(_.run.getOrThrow)).result()
      }

  private[zio] final class Impl[A] private[zio] (override val underlying: Promise[A]) extends ZAsync[A] {

    override def run: ZAsync.Result[NoEffects, A] =
      Try(underlying.get())
        .fold[ZAsync.Result[NoEffects, A]](
          ZAsync.Failure(_),
          ZAsync.Success(_)
        )

    override def run(timeout: Duration): ZAsync.Result[Timeout, A] =
      Try(underlying.get(timeout.toNanos, TimeUnit.NANOSECONDS))
        .fold[ZAsync.Result[Timeout, A]](
          {
            case timeout: TimeoutException => ZAsync.TimedOut(timeout)
            case error                     => ZAsync.Failure(error)
          },
          ZAsync.Success(_)
        )

    override def runCancellable: ZAsync.Result[Cancel, A] =
      Try(underlying.cancellableGet())
        .fold[ZAsync.Result[Cancel, A]](
          {
            case e: CanceledFailure => ZAsync.Cancelled(e)
            case error              => ZAsync.Failure(error)
          },
          ZAsync.Success(_)
        )

    override def runCancellable(timeout: Duration): Result[Cancel with Timeout, A] =
      Try(underlying.cancellableGet(timeout.toNanos, TimeUnit.NANOSECONDS))
        .fold[ZAsync.Result[Cancel with Timeout, A]](
          {
            case e: CanceledFailure        => ZAsync.Cancelled(e)
            case timeout: TimeoutException => ZAsync.TimedOut(timeout)
            case error                     => ZAsync.Failure(error)
          },
          ZAsync.Success(_)
        )

    override def map[B](f: A => B): ZAsync[B] =
      new Impl[B](underlying.thenApply(in => f(in)))

    override def flatMap[B](f: A => ZAsync[B]): ZAsync[B] =
      new Impl[B](underlying.thenCompose { result =>
        f(result).underlying
      })

    override def catchAll[A0 >: A](f: Throwable => ZAsync[A0]): ZAsync[A0] = {
      val thunk = (error: Throwable) => f(error).underlying.get()
      new Impl[A0](
        // java lambdas...
        underlying
          .exceptionally(func1(thunk).asInstanceOf[Functions.Func1[Throwable, _ <: A]])
          .asInstanceOf[Promise[A0]]
      )
    }

    override def catchSome[A0 >: A](pf: PartialFunction[Throwable, ZAsync[A0]]): ZAsync[A0] = {
      val thunk = (error: Throwable) => pf.applyOrElse[Throwable, ZAsync[A0]](error, ZAsync.fail).underlying.get()
      new Impl[A0](
        // java lambdas...
        underlying
          .exceptionally(func1(thunk).asInstanceOf[Functions.Func1[Throwable, _ <: A]])
          .asInstanceOf[Promise[A0]]
      )
    }
  }

  final object Result {

    final implicit class AllEffectsOps[A](private val self: Result[Cancel with Timeout, A]) extends AnyVal {

      def foldAll[B](onTimeout: => B)(cancelled: CanceledFailure => B, failed: Throwable => B, succeeded: A => B): B =
        self match {
          case Success(value)     => succeeded(value)
          case Failure(error)     => failed(error)
          case Cancelled(failure) => cancelled(failure)
          case TimedOut(_)        => onTimeout
        }
    }

    final implicit class CancellableOps[A](private val self: Result[Cancel, A]) extends AnyVal {

      def foldCancel[B](cancelled: CanceledFailure => B, failed: Throwable => B, succeeded: A => B): B =
        self match {
          case Success(value)     => succeeded(value)
          case Failure(error)     => failed(error)
          case Cancelled(failure) => cancelled(failure)
        }
    }

    final implicit class NoEffectsOps[A](private val self: Result[NoEffects, A]) extends AnyVal {

      def foldNoEffects[B](failed: Throwable => B, succeeded: A => B): B =
        self match {
          case Success(value) => succeeded(value)
          case Failure(error) => failed(error)
        }

      def toEither: Either[Throwable, A] = foldNoEffects[Either[Throwable, A]](Left(_), Right(_))
    }

    final implicit class TimeoutOps[A](private val self: Result[Timeout, A]) extends AnyVal {

      def foldTimedOut[B](onTimeout: => B)(failed: Throwable => B, succeeded: A => B): B =
        self match {
          case Success(value) => succeeded(value)
          case Failure(error) => failed(error)
          case TimedOut(_)    => onTimeout
        }
    }
  }
}
